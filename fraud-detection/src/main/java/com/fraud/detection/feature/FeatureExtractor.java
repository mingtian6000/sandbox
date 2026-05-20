package com.fraud.detection.feature;

import com.fraud.detection.cache.CassandraService;
import com.fraud.detection.cache.RedisService;
import com.fraud.detection.model.FeatureVector;
import com.fraud.detection.model.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 特征提取器 — 从 Redis（实时） + Cassandra（历史）读取特征。
 *
 * Redis:   实时预计算特征（每笔交易增量更新，24h 滑动窗口）
 *          → O(1) 读取，始终最新
 *
 * Cassandra: 30-90d 历史聚合指标（夜间 Batch 预计算）
 *          → 读取稳定历史画像
 */
@ApplicationScoped
public class FeatureExtractor {

    private static final Logger log = LoggerFactory.getLogger(FeatureExtractor.class);

    @Inject
    RedisService redisService;

    @Inject
    CassandraService cassandraService;

    public FeatureVector extract(Transaction tx) {
        FeatureVector fv = new FeatureVector();
        String accountId = tx.getAccountId();

        // ================================================================
        // Phase 1: Redis 实时特征（增量更新，始终最新）
        // ================================================================
        Map<String, String> redisFeats = redisService.getFeatures(accountId);

        int    txCount24h       = intFeat(redisFeats, "tx_count_24h", 0);
        double totalAmount24h   = doubleFeat(redisFeats, "total_amount_24h", 0);
        int    merchantCount    = intFeat(redisFeats, "merchant_count", 0);

        double avgAmount24h = txCount24h > 0 ? totalAmount24h / txCount24h : 0;
        double amountZscore = avgAmount24h > 0
            ? (tx.getAmount() - avgAmount24h) / Math.sqrt(avgAmount24h + 1)
            : 0;

        // ================================================================
        // Phase 2: Cassandra 历史指标（30-90d, 夜间Batch更新）
        // ================================================================
        Map<String, Double> cassMetrics = cassandraService.getAccountMetrics(accountId);

        double txCount30d           = cassMetrics.getOrDefault("tx_count_30d", 0.0);
        double totalAmount30d       = cassMetrics.getOrDefault("total_amount_30d", 0.0);
        double amountP99            = cassMetrics.getOrDefault("amount_p99", 5000.0);
        double geoVelocity30d       = cassMetrics.getOrDefault("geo_velocity_30d", 50.0);
        double merchantDiversity30d = cassMetrics.getOrDefault("merchant_diversity_30d", 8.0);
        double nightTxRatio30d      = cassMetrics.getOrDefault("night_tx_ratio_30d", 0.05);
        double deviceChangeCount30d = cassMetrics.getOrDefault("device_change_count_30d", 1.0);

        // ================================================================
        // Phase 3: 计算衍生特征
        // ================================================================
        double amountNormalized = amountP99 > 0 ? tx.getAmount() / amountP99 : 0;
        double deviceRisk       = computeDeviceRisk(tx);
        double ipReputation     = computeIpReputation(tx);
        double timeAnomaly      = computeTimeAnomaly(tx, redisService.getRecentTransactions(accountId, 100));
        double geoVelocity      = geoVelocity30d > 0 ? geoVelocity30d : computeGeoVelocity(tx);

        // ================================================================
        // Phase 4: 组装 FeatureVector（10 维）
        // ================================================================
        return fv
            .setAmountNormalized(   clamp(amountNormalized, 0, 10))
            .setTxCount24h(         txCount24h)
            .setAvgAmount24h(       avgAmount24h)
            .setGeoVelocity(        geoVelocity)
            .setDeviceRiskScore(    deviceRisk)
            .setIpReputation(       ipReputation)
            .setTimeAnomaly(        timeAnomaly)
            .setAmountZscore(       clamp(Math.abs(amountZscore), 0, 10))
            .setMerchantDiversity(  Math.min((int) merchantDiversity30d + merchantCount, 100))
            .setNightTxRatio(       nightTxRatio30d);
    }

    // ====================================================================
    //  辅助方法
    // ====================================================================

    private double computeDeviceRisk(Transaction tx) {
        String deviceId = tx.getDeviceId();
        if (deviceId == null || deviceId.isBlank()) return 0.5;
        if (deviceId.startsWith("emulator-") || deviceId.startsWith("genymotion-")) return 0.8;
        if (deviceId.startsWith("unknown-")) return 0.4;

        // Redis 中查此设备关联了多少账户
        int accountCount = redisService.getDeviceAccountCount(deviceId);
        if (accountCount > 5) return 0.7;  // 设备被多账户共用（设备农场）

        return 0.1;
    }

    private double computeIpReputation(Transaction tx) {
        String ip = tx.getIpAddress();
        if (ip == null || ip.isBlank()) return 0.3;
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("172.")) return 0.7;
        return 0.8;
    }

    private double computeTimeAnomaly(Transaction tx, java.util.List<Transaction> recent) {
        long hour = java.time.Instant.ofEpochMilli(tx.getTimestamp())
            .atZone(java.time.ZoneId.systemDefault()).getHour();
        boolean isNight = hour >= 23 || hour <= 5;

        if (!isNight) return 0.05;
        if (recent.isEmpty()) return 0.2;

        long nightCount = recent.stream()
            .map(t -> java.time.Instant.ofEpochMilli(t.getTimestamp())
                .atZone(java.time.ZoneId.systemDefault()).getHour())
            .filter(h -> h >= 23 || h <= 5).count();

        double nightRatio = (double) nightCount / recent.size();
        return nightRatio < 0.1 ? 0.4 : 0.1;
    }

    private double computeGeoVelocity(Transaction tx) {
        return 0; // 简化，实际从Redis取上次地理位置
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private int intFeat(Map<String, String> m, String key, int def) {
        try { return Integer.parseInt(m.getOrDefault(key, String.valueOf(def))); }
        catch (Exception e) { return def; }
    }

    private double doubleFeat(Map<String, String> m, String key, double def) {
        try { return Double.parseDouble(m.getOrDefault(key, String.valueOf(def))); }
        catch (Exception e) { return def; }
    }
}
