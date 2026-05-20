package com.fraud.detection.batch;

import com.fraud.detection.model.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * 单账户的聚合指标 — 夜间 Batch 计算产出。
 * 写入 Cassandra，供给第二天实时评分使用。
 */
public class AccountMetrics {

    private String accountId;
    private long computedAt;

    // 交易频次
    private double txCount30d;
    private double txCount7d;
    private double txCount24h;

    // 交易金额
    private double totalAmount30d;
    private double avgAmount30d;
    private double amountP99;
    private double amountP95;
    private double amountP50;
    private double maxAmount30d;
    private double minAmount30d;

    // 行为特征
    private double geoVelocity30d;
    private double merchantDiversity30d;
    private double deviceChangeCount30d;
    private double nightTxRatio30d;

    // 时间窗口
    private long windowStart30d;
    private long windowEnd30d;

    public AccountMetrics() {}

    // ---- builder ----
    public AccountMetrics accountId(String v)            { this.accountId = v; return this; }
    public AccountMetrics computedAt(long v)             { this.computedAt = v; return this; }
    public AccountMetrics txCount30d(double v)           { this.txCount30d = v; return this; }
    public AccountMetrics txCount7d(double v)            { this.txCount7d = v; return this; }
    public AccountMetrics txCount24h(double v)           { this.txCount24h = v; return this; }
    public AccountMetrics totalAmount30d(double v)       { this.totalAmount30d = v; return this; }
    public AccountMetrics avgAmount30d(double v)         { this.avgAmount30d = v; return this; }
    public AccountMetrics amountP99(double v)            { this.amountP99 = v; return this; }
    public AccountMetrics amountP95(double v)            { this.amountP95 = v; return this; }
    public AccountMetrics amountP50(double v)            { this.amountP50 = v; return this; }
    public AccountMetrics maxAmount30d(double v)         { this.maxAmount30d = v; return this; }
    public AccountMetrics minAmount30d(double v)         { this.minAmount30d = v; return this; }
    public AccountMetrics geoVelocity30d(double v)       { this.geoVelocity30d = v; return this; }
    public AccountMetrics merchantDiversity30d(double v) { this.merchantDiversity30d = v; return this; }
    public AccountMetrics deviceChangeCount30d(double v) { this.deviceChangeCount30d = v; return this; }
    public AccountMetrics nightTxRatio30d(double v)      { this.nightTxRatio30d = v; return this; }
    public AccountMetrics window(long start, long end)   { this.windowStart30d = start; this.windowEnd30d = end; return this; }

    // ---- 转换为 Map（用于 Cassandra 写入） ----
    public Map<String, Double> toMetricsMap() {
        Map<String, Double> m = new HashMap<>();
        m.put("tx_count_30d", txCount30d);
        m.put("tx_count_7d", txCount7d);
        m.put("tx_count_24h", txCount24h);
        m.put("total_amount_30d", totalAmount30d);
        m.put("avg_amount_30d", avgAmount30d);
        m.put("amount_p99", amountP99);
        m.put("amount_p95", amountP95);
        m.put("amount_p50", amountP50);
        m.put("max_amount_30d", maxAmount30d);
        m.put("min_amount_30d", minAmount30d);
        m.put("geo_velocity_30d", geoVelocity30d);
        m.put("merchant_diversity_30d", merchantDiversity30d);
        m.put("device_change_count_30d", deviceChangeCount30d);
        m.put("night_tx_ratio_30d", nightTxRatio30d);
        return m;
    }

    // ---- getters ----
    public String getAccountId()          { return accountId; }
    public long getComputedAt()           { return computedAt; }
    public double getTxCount30d()         { return txCount30d; }
    public double getTxCount7d()          { return txCount7d; }
    public double getTxCount24h()         { return txCount24h; }
    public double getTotalAmount30d()     { return totalAmount30d; }
    public double getAvgAmount30d()       { return avgAmount30d; }
    public double getAmountP99()          { return amountP99; }
    public double getAmountP95()          { return amountP95; }
    public double getAmountP50()          { return amountP50; }
    public double getMaxAmount30d()       { return maxAmount30d; }
    public double getMinAmount30d()       { return minAmount30d; }
    public double getGeoVelocity30d()     { return geoVelocity30d; }
    public double getMerchantDiversity30d() { return merchantDiversity30d; }
    public double getDeviceChangeCount30d() { return deviceChangeCount30d; }
    public double getNightTxRatio30d()    { return nightTxRatio30d; }
    public long getWindowStart30d()       { return windowStart30d; }
    public long getWindowEnd30d()         { return windowEnd30d; }

    @Override
    public String toString() {
        return String.format(
            "AccountMetrics[%s: tx30d=%.0f, amt30d=%.0f, p99=%.0f, devChg=%.0f, night=%.2f]",
            accountId, txCount30d, totalAmount30d, amountP99,
            deviceChangeCount30d, nightTxRatio30d);
    }
}
