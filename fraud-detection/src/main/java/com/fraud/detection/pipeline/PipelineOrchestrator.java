package com.fraud.detection.pipeline;

import com.fraud.detection.model.*;
import com.fraud.detection.scanner.Scanner;
import com.fraud.detection.rule.JaninoRuleEngine;
import com.fraud.detection.feature.FeatureExtractor;
import com.fraud.detection.feature.FeatureMerger;
import com.fraud.detection.model.OnnxModelService;
import com.fraud.detection.decision.DecisionMaker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * DAG-based scoring pipeline orchestrator.
 *
 * Flow:
 *   Transaction → DataLoader → [Scanner | Rules | Model] (parallel)
 *   → FeatureMerger → ScoreAggregator → DecisionMaker → ScoringResult
 */
@ApplicationScoped
public class PipelineOrchestrator {

    @Inject
    List<Scanner> scanners;

    @Inject
    JaninoRuleEngine ruleEngine;

    @Inject
    FeatureExtractor featureExtractor;

    @Inject
    FeatureMerger featureMerger;

    @Inject
    OnnxModelService modelService;

    @Inject
    DecisionMaker decisionMaker;

    static final long STAGE_TIMEOUT_MS = 500;

    public ScoringResult execute(Transaction tx) {
        long start = System.currentTimeMillis();
        PipelineContext ctx = new PipelineContext(tx);

        try {
            // ---- Phase 1: Feature Extraction (from Redis + Cassandra) ----
            FeatureVector features = featureExtractor.extract(tx);
            ctx.setFeatures(features);

            // ---- Phase 2: Parallel Pipeline (Scanner | Rules | Model) ----
            CompletableFuture<List<ScannerResult>> scannerFuture =
                CompletableFuture.supplyAsync(() -> runScanners(tx, ctx))
                    .orTimeout(STAGE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> { ctx.addWarning("scanners", ex.getMessage()); return List.of(); });

            CompletableFuture<List<RuleResult>> rulesFuture =
                CompletableFuture.supplyAsync(() -> ruleEngine.evaluate(tx, features, ctx))
                    .orTimeout(STAGE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> { ctx.addWarning("rules", ex.getMessage()); return List.of(); });

            CompletableFuture<Double> modelFuture =
                CompletableFuture.supplyAsync(() -> modelService.score(features))
                    .orTimeout(STAGE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> { ctx.addWarning("model", ex.getMessage()); return 0.0; });

            // Wait for all three tracks
            CompletableFuture.allOf(scannerFuture, rulesFuture, modelFuture).join();

            List<ScannerResult> scannerResults = scannerFuture.getNow(List.of());
            List<RuleResult>   ruleResults   = rulesFuture.getNow(List.of());
            double             modelScore    = modelFuture.getNow(0.0);

            // ---- Phase 3: Feature Merger (Janino) ----
            double scannersScore = aggregateScannerScore(scannerResults);
            double rulesScore    = aggregateRuleScore(ruleResults);
            double finalScore    = featureMerger.merge(modelScore, rulesScore, scannersScore, ctx);

            // ---- Phase 4: Decision ----
            DecisionMaker.Decision decision = decisionMaker.decide(finalScore, ctx);

            // ---- Build Result ----
            long elapsed = System.currentTimeMillis() - start;
            return ScoringResult.create(tx.getTransactionId())
                .withFraudScore(finalScore)
                .withDecision(decision.name())
                .withDecisionReason(decision.getReason())
                .withModelScore(modelScore)
                .withRulesScore(rulesScore)
                .withScannersScore(scannersScore)
                .withScannerDetails(scannerResults)
                .withRuleDetails(ruleResults)
                .withProcessingTime(elapsed)
                .withPipelineVersion("1.0.0");

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return ScoringResult.create(tx.getTransactionId())
                .withFraudScore(50.0)
                .withDecision("PENDING")
                .withDecisionReason("Pipeline error: " + e.getMessage())
                .withProcessingTime(elapsed)
                .withPipelineVersion("1.0.0");
        }
    }

    private List<ScannerResult> runScanners(Transaction tx, PipelineContext ctx) {
        List<ScannerResult> results = new ArrayList<>();
        for (Scanner scanner : scanners) {
            try {
                results.add(scanner.scan(tx, ctx));
            } catch (Exception e) {
                ctx.addWarning(scanner.getName(), "Scanner failed: " + e.getMessage());
                results.add(ScannerResult.create(scanner.getName())
                    .withRiskScore(0.0).withRiskLevel("LOW")
                    .withReasonCode("ERROR").withDetail(e.getMessage()));
            }
        }
        return results;
    }

    private double aggregateScannerScore(List<ScannerResult> results) {
        if (results.isEmpty()) return 0;
        double sum = 0;
        for (ScannerResult r : results) sum += r.getRiskScore();
        return (sum / results.size()) * 100;
    }

    private double aggregateRuleScore(List<RuleResult> results) {
        if (results.isEmpty()) return 0;
        double sum = 0;
        for (RuleResult r : results) {
            if (r.isTriggered()) sum += r.getRuleWeight();
        }
        return Math.min(sum, 100);
    }
}
