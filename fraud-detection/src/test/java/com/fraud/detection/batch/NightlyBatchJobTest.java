package com.fraud.detection.batch;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 夜间 Batch 逻辑测试 — 不依赖 Redis/Cassandra，直接测试 computeDailyMetrics + mergeRolling。
 */
@QuarkusTest
class NightlyBatchJobTest {

    @Inject
    NightlyBatchJob batchJob;

    // ====================================================================
    //  computeDailyMetrics 测试
    // ====================================================================

    @Test
    @DisplayName("空交易列表 → 全零指标")
    void testEmptyTransactions() {
        var metrics = batchJob.computeDailyMetrics("acc_test_001", List.of());
        assertEquals(0, metrics.getTxCount30d());
        assertEquals(0, metrics.getTotalAmount30d());
        assertEquals(0, metrics.getAmountP99());
    }

    @Test
    @DisplayName("单笔交易 → 所有金额指标一致")
    void testSingleTransaction() {
        var txns = List.of(makeTx(100.0));
        var m = batchJob.computeDailyMetrics("acc_test_001", txns);

        assertEquals(1, m.getTxCount30d());
        assertEquals(100.0, m.getTotalAmount30d(), 0.01);
        assertEquals(100.0, m.getAvgAmount30d(), 0.01);
        assertEquals(100.0, m.getAmountP99(), 0.01);
        assertEquals(100.0, m.getAmountP50(), 0.01);
        assertEquals(100.0, m.getMaxAmount30d(), 0.01);
        assertEquals(100.0, m.getMinAmount30d(), 0.01);
        assertEquals(1, m.getMerchantDiversity30d(), 0.01);
        assertEquals(1, m.getDeviceChangeCount30d(), 0.01);
    }

    @Test
    @DisplayName("多笔交易 → 正确计算分位数和聚合值")
    void testMultipleTransactions() {
        var txns = List.of(
            makeTx(10.0, "merchant_a", "device_1"),
            makeTx(20.0, "merchant_b", "device_1"),
            makeTx(30.0, "merchant_a", "device_2"),
            makeTx(100.0, "merchant_c", "device_1"),
            makeTx(500.0, "merchant_d", "device_3")
        );
        var m = batchJob.computeDailyMetrics("acc_test_001", txns);

        assertEquals(5, m.getTxCount30d());
        assertEquals(660.0, m.getTotalAmount30d(), 0.01);
        assertEquals(132.0, m.getAvgAmount30d(), 0.01);
        assertEquals(500.0, m.getMaxAmount30d(), 0.01);
        assertEquals(10.0, m.getMinAmount30d(), 0.01);
        assertEquals(4, m.getMerchantDiversity30d(), 0.01);
        assertEquals(3, m.getDeviceChangeCount30d(), 0.01);
    }

    @Test
    @DisplayName("夜间交易比例计算正确")
    void testNightTxRatio() {
        long nightHour = 1711929600000L + 2 * 3600000;  // 2AM
        long dayHour  = 1711929600000L + 14 * 3600000;  // 2PM

        var txns = List.of(
            makeTxAt(100.0, nightHour),
            makeTxAt(200.0, nightHour),
            makeTxAt(300.0, dayHour),
            makeTxAt(400.0, dayHour)
        );
        var m = batchJob.computeDailyMetrics("acc_test_001", txns);

        assertEquals(0.5, m.getNightTxRatio30d(), 0.01);
    }

    @Test
    @DisplayName("99th percentile 计算正确")
    void testPercentile() {
        var txns = List.of(
            makeTx(1), makeTx(2), makeTx(3), makeTx(4), makeTx(5),
            makeTx(6), makeTx(7), makeTx(8), makeTx(9), makeTx(100)
        );
        var m = batchJob.computeDailyMetrics("acc_test_001", txns);

        assertTrue(m.getAmountP99() > 50, "P99 should be closer to max");
        assertTrue(m.getAmountP99() <= 100, "P99 should not exceed max");
    }

    // ====================================================================
    //  mergeRolling 测试
    // ====================================================================

    @Test
    @DisplayName("无历史记录 → 直接使用今日数据")
    void testMergeNoHistory() {
        var today = new AccountMetrics()
            .accountId("acc_test").txCount30d(100).totalAmount30d(50000);

        var merged = batchJob.mergeRolling("acc_test", Map.of(), today);

        assertEquals(100, merged.getTxCount30d(), 0.01);
        assertEquals(50000, merged.getTotalAmount30d(), 0.01);
    }

    @Test
    @DisplayName("有历史记录 → 按 29/30 + 1/30 加权合并")
    void testMergeWithHistory() {
        Map<String, Double> existing = Map.of(
            "tx_count_30d", 900.0,
            "total_amount_30d", 450000.0,
            "avg_amount_30d", 500.0
        );
        var today = new AccountMetrics()
            .accountId("acc_test").txCount30d(100).totalAmount30d(60000).avgAmount30d(600);

        var merged = batchJob.mergeRolling("acc_test", existing, today);

        double expectedCount = 900 * 29.0/30 + 100 * 1.0/30;
        double expectedAmount = 450000 * 29.0/30 + 60000 * 1.0/30;

        assertEquals(expectedCount, merged.getTxCount30d(), 0.1);
        assertEquals(expectedAmount, merged.getTotalAmount30d(), 0.1);
    }

    @Test
    @DisplayName("max_amount 取两者最大值，min_amount 取两者最小值")
    void testMinMaxMerge() {
        Map<String, Double> existing = Map.of("max_amount_30d", 5000.0, "min_amount_30d", 10.0);
        var today = new AccountMetrics()
            .accountId("acc_test").maxAmount30d(8000).minAmount30d(5);

        var merged = batchJob.mergeRolling("acc_test", existing, today);

        assertEquals(8000, merged.getMaxAmount30d(), 0.01);
        assertEquals(5, merged.getMinAmount30d(), 0.01);
    }

    // ====================================================================
    //  Helpers
    // ====================================================================

    private com.fraud.detection.model.Transaction makeTx(double amount) {
        return makeTx(amount, "merchant_x", "device_x");
    }

    private com.fraud.detection.model.Transaction makeTx(double amount, String merchant, String device) {
        return makeTxAt(amount, System.currentTimeMillis(), merchant, device);
    }

    private com.fraud.detection.model.Transaction makeTxAt(double amount, long ts) {
        return makeTxAt(amount, ts, "merchant_x", "device_x");
    }

    private com.fraud.detection.model.Transaction makeTxAt(double amount, long ts,
                                                           String merchant, String device) {
        return new com.fraud.detection.model.Transaction()
            .setTransactionId("txn-test-" + System.nanoTime())
            .setAccountId("acc_test")
            .setAmount(amount)
            .setMerchantId(merchant)
            .setDeviceId(device)
            .setTimestamp(ts);
    }
}
