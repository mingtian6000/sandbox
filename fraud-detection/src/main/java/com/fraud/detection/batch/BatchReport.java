package com.fraud.detection.batch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nightly batch run report — one per execution.
 */
public class BatchReport {

    private final long startTime;
    private long endTime;
    private int accountsProcessed;
    private int accountsUpdated;
    private int totalTransactionsScanned;
    private final List<AccountSummary> summaries = new ArrayList<>();
    private final Map<String, Object> metrics = new LinkedHashMap<>();

    public BatchReport() {
        this.startTime = System.currentTimeMillis();
    }

    public void finish() {
        this.endTime = System.currentTimeMillis();
    }

    public BatchReport accountProcessed(String id, int txnCount, Map<String, Double> metricsBefore,
                                        Map<String, Double> metricsAfter) {
        accountsProcessed++;
        totalTransactionsScanned += txnCount;
        summaries.add(new AccountSummary(id, txnCount, metricsBefore, metricsAfter));
        return this;
    }

    public BatchReport accountUpdated(String id) { accountsUpdated++; return this; }

    public long getElapsedMs() { return endTime - startTime; }
    public int getAccountsProcessed() { return accountsProcessed; }
    public int getAccountsUpdated() { return accountsUpdated; }
    public int getTotalTransactionsScanned() { return totalTransactionsScanned; }
    public List<AccountSummary> getSummaries() { return summaries; }

    /** Print summary to log. */
    @Override
    public String toString() {
        return String.format(
            "BatchReport{accounts=%d, updated=%d, txns=%d, elapsed=%dms}",
            accountsProcessed, accountsUpdated, totalTransactionsScanned, getElapsedMs());
    }

    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════════╗\n");
        sb.append("║        NIGHTLY BATCH REPORT                  ║\n");
        sb.append("╚══════════════════════════════════════════════╝\n");
        sb.append(String.format("  Accounts processed:  %d\n", accountsProcessed));
        sb.append(String.format("  Accounts updated:    %d\n", accountsUpdated));
        sb.append(String.format("  Transactions scanned:%d\n", totalTransactionsScanned));
        sb.append(String.format("  Elapsed time:        %dms\n", getElapsedMs()));
        sb.append(String.format("  Rate:                %.0f txn/s\n",
            totalTransactionsScanned / (getElapsedMs() / 1000.0 + 0.001)));

        if (!summaries.isEmpty()) {
            sb.append("\n  ── Top accounts ──\n");
            summaries.stream().limit(5).forEach(s ->
                sb.append(String.format("  %-20s  txns=%-4d  before=%-10s  after=%-10s\n",
                    s.accountId, s.txnCount,
                    formatMap(s.metricsBefore), formatMap(s.metricsAfter))));
        }
        return sb.toString();
    }

    private String formatMap(Map<String, Double> m) {
        if (m == null || m.isEmpty()) return "-";
        return String.format("tx=%.0f amt=%.0f",
            m.getOrDefault("tx_count_30d", 0.0),
            m.getOrDefault("total_amount_30d", 0.0));
    }

    public static class AccountSummary {
        public final String accountId;
        public final int txnCount;
        public final Map<String, Double> metricsBefore;
        public final Map<String, Double> metricsAfter;

        public AccountSummary(String id, int txns, Map<String, Double> before, Map<String, Double> after) {
            this.accountId = id; this.txnCount = txns;
            this.metricsBefore = before; this.metricsAfter = after;
        }
    }
}
