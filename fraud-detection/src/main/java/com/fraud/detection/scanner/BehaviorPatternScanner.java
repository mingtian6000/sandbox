package com.fraud.detection.scanner;

import com.fraud.detection.model.ScannerResult;
import com.fraud.detection.model.Transaction;
import com.fraud.detection.pipeline.PipelineContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Scans for behavioral pattern anomalies.
 * Checks: transaction amount vs history, time of day, channel preference, velocity.
 */
@ApplicationScoped
public class BehaviorPatternScanner implements Scanner {

    @Override
    public String getName() { return "behavior_pattern"; }

    @Override
    public ScannerResult scan(Transaction tx, PipelineContext ctx) {
        ScannerResult result = ScannerResult.create(getName());
        double totalScore = 0;
        int checks = 0;

        // Check 1: Unusual transaction amount
        Double avgAmount = ctx.getAttribute("avg_amount_24h");
        if (avgAmount != null && avgAmount > 0) {
            checks++;
            double ratio = tx.getAmount() / avgAmount;
            if (ratio > 5) {
                totalScore += 0.3;  // >5x average
            } else if (ratio > 3) {
                totalScore += 0.15;
            }
        }

        // Check 2: Night time transaction (11PM - 5AM)
        long hour = java.time.Instant.ofEpochMilli(tx.getTimestamp())
            .atZone(java.time.ZoneId.systemDefault()).getHour();
        if (hour >= 23 || hour <= 5) {
            checks++;
            Double nightRatio = ctx.getAttribute("night_tx_ratio");
            if (nightRatio != null && nightRatio < 0.1) {
                totalScore += 0.25; // Unusual night activity
            }
        }

        // Check 3: Channel mismatch
        String preferredChannel = ctx.getAttribute("preferred_channel");
        if (preferredChannel != null && !preferredChannel.equals(tx.getChannel())) {
            checks++;
            totalScore += 0.1;
        }

        // Check 4: High transaction velocity
        Integer txCount24h = ctx.getAttribute("tx_count_24h");
        if (txCount24h != null && txCount24h > 10) {
            checks++;
            totalScore += 0.2;
        }

        double finalScore = checks > 0 ? totalScore / checks : 0.05;
        String level = finalScore > 0.25 ? "HIGH" : (finalScore > 0.1 ? "MEDIUM" : "LOW");

        return result.withRiskScore(Math.min(finalScore, 1.0))
            .withRiskLevel(level)
            .withReasonCode(finalScore > 0.25 ? "BEHAVIOR_ANOMALY" : "BEHAVIOR_NORMAL")
            .withDetail(String.format("Behavior score: %.2f (%.0f checks)", finalScore, (double)checks));
    }
}
