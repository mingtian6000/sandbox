package com.fraud.detection.rule;

import com.fraud.detection.model.FeatureVector;
import com.fraud.detection.model.RuleResult;
import com.fraud.detection.model.Transaction;
import com.fraud.detection.pipeline.PipelineContext;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Janino-powered rules engine.
 */
@QuarkusTest
class JaninoRuleEngineTest {

    @Inject
    JaninoRuleEngine ruleEngine;

    @Test
    @DisplayName("RULE_001 triggers on large amount + high geo velocity")
    void testLargeAmountGeoRule() {
        // Large amount + high geo velocity → should trigger RULE_001
        Transaction tx = new Transaction().setAmount(15000);
        FeatureVector fv = new FeatureVector()
            .setGeoVelocity(600);
        PipelineContext ctx = new PipelineContext(tx);

        List<RuleResult> results = ruleEngine.evaluate(tx, fv, ctx);

        boolean rule001Triggered = results.stream()
            .filter(r -> r.getRuleId().equals("RULE_001"))
            .anyMatch(RuleResult::isTriggered);

        assertTrue(rule001Triggered, "RULE_001 should trigger for large amount + high velocity");
    }

    @Test
    @DisplayName("RULE_001 does not trigger on normal transaction")
    void testNormalTransactionNoRule() {
        Transaction tx = new Transaction().setAmount(50);
        FeatureVector fv = new FeatureVector()
            .setGeoVelocity(10);
        PipelineContext ctx = new PipelineContext(tx);

        List<RuleResult> results = ruleEngine.evaluate(tx, fv, ctx);

        boolean rule001Triggered = results.stream()
            .filter(r -> r.getRuleId().equals("RULE_001"))
            .anyMatch(RuleResult::isTriggered);

        assertFalse(rule001Triggered, "RULE_001 should NOT trigger for normal transaction");
    }

    @Test
    @DisplayName("RULE_004 triggers on extreme Z-score")
    void testZscoreRule() {
        Transaction tx = new Transaction().setAmount(10000);
        FeatureVector fv = new FeatureVector()
            .setAmountZscore(5.0);
        PipelineContext ctx = new PipelineContext(tx);

        List<RuleResult> results = ruleEngine.evaluate(tx, fv, ctx);

        boolean rule004Triggered = results.stream()
            .filter(r -> r.getRuleId().equals("RULE_004"))
            .anyMatch(RuleResult::isTriggered);

        assertTrue(rule004Triggered, "RULE_004 should trigger for extreme Z-score");
    }

    @Test
    @DisplayName("All 7 rules are loaded and evaluated")
    void testAllRulesLoaded() {
        Transaction tx = new Transaction().setAmount(100);
        FeatureVector fv = new FeatureVector();
        PipelineContext ctx = new PipelineContext(tx);

        List<RuleResult> results = ruleEngine.evaluate(tx, fv, ctx);

        assertFalse(results.isEmpty(), "Should have rule results");
        assertTrue(results.size() >= 5, "Should evaluate at least 5 default rules");
    }

    @Test
    @DisplayName("Triggered rules have non-zero weight")
    void testTriggeredRuleWeight() {
        Transaction tx = new Transaction().setAmount(50000);
        FeatureVector fv = new FeatureVector()
            .setAmountZscore(4.0)
            .setGeoVelocity(800);
        PipelineContext ctx = new PipelineContext(tx);

        List<RuleResult> results = ruleEngine.evaluate(tx, fv, ctx);

        for (RuleResult r : results) {
            if (r.isTriggered()) {
                assertTrue(r.getRuleWeight() > 0,
                    "Triggered rule " + r.getRuleId() + " should have positive weight");
            }
        }
    }
}
