package com.fraud.detection.batch;

import com.fraud.detection.cache.CassandraService;
import com.fraud.detection.cache.RedisService;
import com.fraud.detection.model.Transaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 夜间批处理 — 每天凌晨 02:00 执行。
 *
 * 功能：
 *   ① 从 Redis 读取过去 24h 原始交易数据
 *   ② 按账户聚合：统计每笔交易的特征（金额、设备、地理位置、时间...）
 *   ③ 读取 Cassandra 现有 30 天指标（前 N-1 天的数据）
 *   ④ 滚动窗口合并：丢弃最旧一天 + 加入最新一天 = 新的 30 天指标
 *   ⑤ 写回 Cassandra
 *   ⑥ 生成 BatchReport
 *
 * 输出（写入 Cassandra）：
 *   account_id → {
 *     tx_count_30d, tx_count_7d, tx_count_24h,
 *     total_amount_30d, avg_amount_30d,
 *     amount_p99, amount_p95, amount_p50,
 *     max_amount_30d, min_amount_30d,
 *     geo_velocity_30d, merchant_diversity_30d,
 *     device_change_count_30d, night_tx_ratio_30d
 *   }
 */
@ApplicationScoped
public class NightlyBatchJob {

    private static final Logger log = LoggerFactory.getLogger(NightlyBatchJob.class);

    @Inject
    RedisService redisService;

    @Inject
    CassandraService cassandraService;

    private BatchReport previousReport;

    public BatchReport getLastReport() { return previousReport; }

    /**
     * 每天凌晨 2:00 执行。
     * cron: 秒 分 时 日 月 星期
     */
    @Scheduled(cron = "0 0 2 * * ?", identity = "nightly-batch")
    void runNightlyBatch() {
        long start = System.currentTimeMillis();
        log.info("═══════════════════════════════════════════");
        log.info("  🌙 Nightly Batch Job — START");
        log.info("═══════════════════════════════════════════");

        BatchReport report = new BatchReport();

        try {
            // ================================================================
            // Phase 1: 收集所有有活动的账户 ID
            // ================================================================
            Set<String> activeAccounts = redisService.getActiveAccounts();
            log.info("Found {} active accounts in Redis", activeAccounts.size());

            if (activeAccounts.isEmpty()) {
                log.warn("No active accounts found. Is Redis populated?");
                report.finish();
                this.previousReport = report;
                log.info("Batch complete (empty): {}", report);
                return;
            }

            // ================================================================
            // Phase 2: 处理每个账户
            // ================================================================
            for (String accountId : activeAccounts) {
                try {
                    processAccount(accountId, report);
                } catch (Exception e) {
                    log.error("Failed to process account {}: {}", accountId, e.getMessage());
                }
            }

            report.finish();

            // ================================================================
            // Phase 3: 记录并保存报告
            // ================================================================
            this.previousReport = report;
            log.info(report.toPrettyString());

        } catch (Exception e) {
            log.error("Nightly batch job failed", e);
            report.finish();
            this.previousReport = report;
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("═══════════════════════════════════════════");
        log.info("  🌙 Nightly Batch — END ({}ms)", elapsed);
        log.info("═══════════════════════════════════════════");
    }

    /**
     * 处理单个账户：
     *   读 Redis 最近数据 → 计算今日指标 → 合并 Cassandra 历史指标 → 写入
     */
    private void processAccount(String accountId, BatchReport report) {
        // ---------------------------------------------------------------
        // Step 1: 从 Redis 读取该账户最近 1000 笔交易
        // ---------------------------------------------------------------
        List<Transaction> recentTxns = redisService.getRecentTransactions(accountId, 1000);
        if (recentTxns.isEmpty()) {
            log.debug("Account {} has no recent transactions, skipping", accountId);
            return;
        }

        // ---------------------------------------------------------------
        // Step 2: 读取 Cassandra 现有指标（前 29 天的数据）
        // ---------------------------------------------------------------
        Map<String, Double> existingMetrics = cassandraService.getAccountMetrics(accountId);
        Map<String, Double> metricsBefore = new HashMap<>(existingMetrics);

        // ---------------------------------------------------------------
        // Step 3: 计算今日聚合指标
        // ---------------------------------------------------------------
        AccountMetrics todayMetrics = computeDailyMetrics(accountId, recentTxns);

        // ---------------------------------------------------------------
        // Step 4: 滚动合并 — 新 30 天指标 = 历史 × (29/30) + 今日 × (1/30)
        // ---------------------------------------------------------------
        AccountMetrics merged = mergeRolling(accountId, existingMetrics, todayMetrics);

        // ---------------------------------------------------------------
        // Step 5: 写回 Cassandra
        // ---------------------------------------------------------------
        cassandraService.updateMetrics(accountId, merged.toMetricsMap());

        report.accountProcessed(accountId, recentTxns.size(), metricsBefore, merged.toMetricsMap());
        report.accountUpdated(accountId);
    }

    /**
     * 从原始交易列表计算该账户当天的聚合指标。
     */
    AccountMetrics computeDailyMetrics(String accountId, List<Transaction> txns) {
        AccountMetrics m = new AccountMetrics()
            .accountId(accountId)
            .computedAt(System.currentTimeMillis());

        if (txns.isEmpty()) return m;

        // 基础统计
        int count = txns.size();
        double totalAmount = txns.stream().mapToDouble(Transaction::getAmount).sum();
        double avgAmount = totalAmount / count;
        DoubleSummaryStatistics amtStats = txns.stream()
            .mapToDouble(Transaction::getAmount).summaryStatistics();

        // 金额分位数
        double[] sorted = txns.stream().mapToDouble(Transaction::getAmount).sorted().toArray();
        double p99 = percentile(sorted, 0.99);
        double p95 = percentile(sorted, 0.95);
        double p50 = percentile(sorted, 0.50);

        // 设备多样性
        Set<String> devices = txns.stream()
            .map(Transaction::getDeviceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // 商户多样性
        Set<String> merchants = txns.stream()
            .map(Transaction::getMerchantId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // 夜间交易比例 (23:00 - 05:00)
        double nightCount = txns.stream()
            .filter(tx -> {
                int h = Instant.ofEpochMilli(tx.getTimestamp())
                    .atZone(ZoneId.systemDefault()).getHour();
                return h >= 23 || h <= 5;
            })
            .count();
        double nightRatio = count > 0 ? nightCount / count : 0;

        // 地理位置变化
        double geoVelocity = computeGeoVelocity(txns);

        // 时间窗口
        long minTs = txns.stream().mapToLong(Transaction::getTimestamp).min().orElse(0);
        long maxTs = txns.stream().mapToLong(Transaction::getTimestamp).max().orElse(0);

        return m
            .txCount30d(count)
            .txCount7d(count)        // simplified: use all for POC
            .txCount24h(count)
            .totalAmount30d(totalAmount)
            .avgAmount30d(avgAmount)
            .amountP99(p99)
            .amountP95(p95)
            .amountP50(p50)
            .maxAmount30d(amtStats.getMax())
            .minAmount30d(amtStats.getMin())
            .geoVelocity30d(geoVelocity)
            .merchantDiversity30d(merchants.size())
            .deviceChangeCount30d(devices.size())
            .nightTxRatio30d(nightRatio)
            .window(minTs, maxTs);
    }

    /**
     * 滚动窗口合并：
     *   new_metric = old_metric × (29/30) + today_metric × (1/30)
     *
     * 这样不用存 30 天的明细数据，只需要一个递推公式。
     * 对于新账户（无历史），直接使用今天的指标。
     */
    AccountMetrics mergeRolling(String accountId, Map<String, Double> existing,
                                AccountMetrics today) {
        double decayOld = 29.0 / 30.0;  // 历史衰减
        double decayNew = 1.0 / 30.0;   // 今日权重

        boolean hasHistory = existing != null && !existing.isEmpty();

        return new AccountMetrics()
            .accountId(accountId)
            .computedAt(System.currentTimeMillis())
            .txCount30d(rolling(existing.get("tx_count_30d"), today.getTxCount30d(), decayOld, decayNew))
            .txCount7d(rolling(existing.get("tx_count_7d"), today.getTxCount7d(), 6.0/7, 1.0/7))
            .txCount24h(today.getTxCount24h())
            .totalAmount30d(rolling(existing.get("total_amount_30d"), today.getTotalAmount30d(), decayOld, decayNew))
            .avgAmount30d(rolling(existing.get("avg_amount_30d"), today.getAvgAmount30d(), decayOld, decayNew))
            .amountP99(rolling(existing.get("amount_p99"), today.getAmountP99(), decayOld, decayNew))
            .amountP95(rolling(existing.get("amount_p95"), today.getAmountP95(), decayOld, decayNew))
            .amountP50(rolling(existing.get("amount_p50"), today.getAmountP50(), decayOld, decayNew))
            .maxAmount30d(Math.max(
                existing.getOrDefault("max_amount_30d", 0.0), today.getMaxAmount30d()))
            .minAmount30d(hasHistory ? Math.min(
                existing.getOrDefault("min_amount_30d", Double.MAX_VALUE), today.getMinAmount30d())
                : today.getMinAmount30d())
            .geoVelocity30d(rolling(existing.get("geo_velocity_30d"), today.getGeoVelocity30d(), decayOld, decayNew))
            .merchantDiversity30d(rolling(existing.get("merchant_diversity_30d"), today.getMerchantDiversity30d(), decayOld, decayNew))
            .deviceChangeCount30d(rolling(existing.get("device_change_count_30d"), today.getDeviceChangeCount30d(), decayOld, decayNew))
            .nightTxRatio30d(rolling(existing.get("night_tx_ratio_30d"), today.getNightTxRatio30d(), decayOld, decayNew))
            .window(today.getWindowStart30d(), today.getWindowEnd30d());
    }

    /** Rolling window merge helper. */
    private double rolling(Double oldVal, double newVal, double decayOld, double decayNew) {
        if (oldVal == null) return newVal;
        return oldVal * decayOld + newVal * decayNew;
    }

    /** Compute percentile from sorted array. */
    private double percentile(double[] sorted, double pct) {
        if (sorted.length == 0) return 0;
        if (sorted.length == 1) return sorted[0];
        double idx = pct * (sorted.length - 1);
        int lo = (int) Math.floor(idx);
        int hi = (int) Math.ceil(idx);
        if (lo == hi) return sorted[lo];
        return sorted[lo] + (idx - lo) * (sorted[hi] - sorted[lo]);
    }

    /** Simplified geo velocity: max distance between consecutive transactions. */
    private double computeGeoVelocity(List<Transaction> txns) {
        if (txns.size() < 2) return 0;
        double maxSpeed = 0;
        for (int i = 1; i < txns.size(); i++) {
            Transaction prev = txns.get(i - 1);
            Transaction cur = txns.get(i);
            String g1 = prev.getGeoLocation();
            String g2 = cur.getGeoLocation();
            if (g1 == null || g2 == null || g1.equals(g2)) continue;

            long timeDiffHours = (cur.getTimestamp() - prev.getTimestamp()) / 3_600_000;
            if (timeDiffHours <= 0) continue;

            double dist = haversine(g1, g2);
            double speed = dist / timeDiffHours;
            if (speed > maxSpeed) maxSpeed = speed;
        }
        return maxSpeed;
    }

    private double haversine(String geo1, String geo2) {
        try {
            String[] p1 = geo1.split(",");
            String[] p2 = geo2.split(",");
            double lat1 = Math.toRadians(Double.parseDouble(p1[0].trim()));
            double lon1 = Math.toRadians(Double.parseDouble(p1[1].trim()));
            double lat2 = Math.toRadians(Double.parseDouble(p2[0].trim()));
            double lon2 = Math.toRadians(Double.parseDouble(p2[1].trim()));
            double dLat = lat2 - lat1, dLon = lon2 - lon1;
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                     + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
            return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        } catch (Exception e) { return 0; }
    }
}
