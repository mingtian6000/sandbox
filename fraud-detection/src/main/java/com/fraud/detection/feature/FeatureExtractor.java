package com.fraud.detection.feature;

import com.fraud.detection.cache.RedisService;
import com.fraud.detection.cache.CassandraService;
import com.fraud.detection.model.FeatureVector;
import com.fraud.detection.model.Transaction;
import com.fraud.detection.pipeline.PipelineContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

/**
 * Extracts features from raw transaction data + Redis + Cassandra.
 *
 * Redis (L1, 24h): raw recent transaction data
 * Cassandra (L2, 30-90d): pre-computed aggregated metrics
 */
@ApplicationScoped
public class FeatureExtractor {

    @Inject
    RedisService redisService;

    @Inject
    CassandraService cassandraService;

    public FeatureVector extract(Transaction tx) {
        FeatureVector fv = new FeatureVector();
        String accountId = tx.getAccountId();

        // ---- Phase 1: Pull raw data from Redis (24h window) ----
        List<Transaction> recentTxns = redisService.getRecentTransactions(accountId, 100);
        int txCount24h = recentTxns.size();
        double avgAmount24h = recentTxns.stream()
            .mapToDouble(Transaction::getAmount).average().orElse(0);
        double maxAmount24h = recentTxns.stream()
            .mapToDouble(Transaction::getAmount).max().orElse(0);

        // ---- Phase 2: Pull aggregated metrics from Cassandra (30-90d) ----
        Map<String, Double> metrics = cassandraService.getAccountMetrics(accountId);
        double txCount30d = metrics.getOrDefault("tx_count_30d", 0.0);
        double totalAmount30d = metrics.getOrDefault("total_amount_30d", 0.0);
        double amountP99 = metrics.getOrDefault("amount_p99", 0.0);
        double geoVelocity30d = metrics.getOrDefault("geo_velocity_30d", 0.0);
        double merchantDiversity30d = metrics.getOrDefault("merchant_diversity_30d", 0.0);

        // ---- Phase 3: Compute derived features ----
        double amountNormalized = amountP99 > 0 ? tx.getAmount() / amountP99 : 0;
        double amountZscore = avgAmount24h > 0
            ? (tx.getAmount() - avgAmount24h) / Math.sqrt(avgAmount24h + 1)
            : 0;
        double deviceRisk = computeDeviceRisk(tx);
        double ipReputation = computeIpReputation(tx);
        double timeAnomaly = computeTimeAnomaly(tx, recentTxns);
        double nightTxRatio = metrics.getOrDefault("night_tx_ratio_30d", 0.0);

        // ---- Set context attributes for scanners ----
        // (set on ctx so scanners don't re-query)
        // Note: ctx is not passed here; scanners query via their own mechanism.
        // Could use a ThreadLocal or shared context if needed.

        // ---- Build FeatureVector ----
        return fv.setAmountNormalized(Math.min(amountNormalized, 10))
            .setTxCount24h(txCount24h)
            .setAvgAmount24h(avgAmount24h)
            .setGeoVelocity(geoVelocity30d > 0 ? geoVelocity30d
                : computeGeoVelocity(recentTxns, tx))
            .setDeviceRiskScore(deviceRisk)
            .setIpReputation(ipReputation)
            .setTimeAnomaly(timeAnomaly)
            .setAmountZscore(Math.min(Math.abs(amountZscore), 10))
            .setMerchantDiversity((int) Math.min(merchantDiversity30d, 100))
            .setNightTxRatio(nightTxRatio);
    }

    private double computeDeviceRisk(Transaction tx) {
        String deviceId = tx.getDeviceId();
        if (deviceId == null || deviceId.isBlank()) return 0.5;
        if (deviceId.startsWith("emulator-") || deviceId.startsWith("genymotion-")) return 0.8;
        if (deviceId.startsWith("unknown-")) return 0.4;
        return 0.1;
    }

    private double computeIpReputation(Transaction tx) {
        String ip = tx.getIpAddress();
        if (ip == null || ip.isBlank()) return 0.3;
        // Check against known proxy/VPN ranges (simplified)
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) return 0.7; // unusual
        // In production: query IP reputation service / Redis blacklist
        return 0.8; // default: assume reputable
    }

    private double computeTimeAnomaly(Transaction tx, List<Transaction> recent) {
        long hour = java.time.Instant.ofEpochMilli(tx.getTimestamp())
            .atZone(java.time.ZoneId.systemDefault()).getHour();
        boolean isNight = hour >= 23 || hour <= 5;

        if (!isNight) return 0.05;
        if (recent.isEmpty()) return 0.2; // first transaction at night
        long recentNightCount = recent.stream()
            .map(t -> java.time.Instant.ofEpochMilli(t.getTimestamp())
                .atZone(java.time.ZoneId.systemDefault()).getHour())
            .filter(h -> h >= 23 || h <= 5).count();
        double nightRatio = (double) recentNightCount / recent.size();
        if (nightRatio < 0.1) return 0.4; // unusual night activity
        return 0.1;
    }

    private double computeGeoVelocity(List<Transaction> recent, Transaction tx) {
        if (recent.isEmpty()) return 0;
        Transaction last = recent.get(recent.size() - 1);
        String lastGeo = last.getGeoLocation();
        String currentGeo = tx.getGeoLocation();
        if (lastGeo == null || currentGeo == null
            || lastGeo.equals(currentGeo)) return 0;
        long timeDiffHours = (tx.getTimestamp() - last.getTimestamp()) / 3_600_000;
        if (timeDiffHours <= 0) return 1000;
        return 500.0 / timeDiffHours; // simplified
    }
}
