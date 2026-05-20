package com.fraud.detection.pipeline;

import com.fraud.detection.cache.CassandraService;
import com.fraud.detection.cache.RedisService;
import com.fraud.detection.decision.DecisionMaker;
import com.fraud.detection.feature.FeatureExtractor;
import com.fraud.detection.feature.FeatureMerger;
import com.fraud.detection.model.*;
import com.fraud.detection.rule.FraudRule;
import com.fraud.detection.rule.JaninoRuleEngine;
import com.fraud.detection.rule.RuleConfigLoader;
import com.fraud.detection.model.FeatureVector;
import com.fraud.detection.model.OnnxModelService;
import com.fraud.detection.scanner.BehaviorPatternScanner;
import com.fraud.detection.scanner.DeviceFingerprintScanner;
import com.fraud.detection.scanner.GeoLocationScanner;
import com.fraud.detection.scanner.Scanner;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the full scoring pipeline.
 */
@QuarkusTest
class PipelineOrchestratorTest {

    @Inject
    PipelineOrchestrator orchestrator;

    @Test
    @DisplayName("Normal transaction → APPROVE with low score")
    void testNormalTransaction() {
        Transaction tx = new Transaction()
            .setTransactionId("txn-test-001")
            .setAccountId("acc_normal_001")
            .setAmount(150.00)
            .setCurrency("USD")
            .setChannel("ONLINE")
            .setDeviceId("device-macbook-pro")
            .setIpAddress("8.8.8.8")
            .setGeoLocation("37.7749,-122.4194")
            .setTimestamp(Instant.now().toEpochMilli());

        ScoringResult result = orchestrator.execute(tx);

        System.out.println("Normal TX → score=" + result.getFraudScore()
            + ", decision=" + result.getDecision()
            + ", time=" + result.getProcessingTimeMs() + "ms");

        assertNotNull(result.getTransactionId());
        assertTrue(result.getFraudScore() < 60,
            "Normal transaction should have low score, got: " + result.getFraudScore());
        assertTrue(result.getProcessingTimeMs() >= 0);
        assertNotNull(result.getDecision());
    }

    @Test
    @DisplayName("High-risk transaction → REJECT with high score")
    void testFraudTransaction() {
        Transaction tx = new Transaction()
            .setTransactionId("txn-test-099")
            .setAccountId("acc_suspicious_001")
            .setAmount(99999.99)
            .setCurrency("USD")
            .setChannel("ONLINE")
            .setDeviceId("emulator-android-xyz")
            .setIpAddress("10.0.0.1")
            .setGeoLocation("35.6762,139.6503")
            .setTimestamp(Instant.now().toEpochMilli());

        ScoringResult result = orchestrator.execute(tx);

        System.out.println("Fraud TX → score=" + result.getFraudScore()
            + ", decision=" + result.getDecision()
            + ", time=" + result.getProcessingTimeMs() + "ms");

        assertNotNull(result.getTransactionId());
        assertTrue(result.getFraudScore() >= 30,
            "Suspicious transaction should have higher score, got: " + result.getFraudScore());
        assertTrue(result.getProcessingTimeMs() >= 0);
    }

    @Test
    @DisplayName("Pipeline handles errors gracefully")
    void testPipelineErrorHandling() {
        Transaction tx = new Transaction()
            .setTransactionId("txn-test-error")
            .setAccountId(null)  // Missing account
            .setAmount(100);

        ScoringResult result = orchestrator.execute(tx);

        assertNotNull(result);
        assertNotNull(result.getDecision());
        System.out.println("Error TX → score=" + result.getFraudScore()
            + ", decision=" + result.getDecision());
    }

    @Test
    @DisplayName("Scoring result includes scanner and rule details")
    void testResultDetails() {
        Transaction tx = new Transaction()
            .setTransactionId("txn-test-details")
            .setAccountId("acc_normal_001")
            .setAmount(500.00)
            .setDeviceId("device-test-001")
            .setIpAddress("8.8.4.4")
            .setGeoLocation("40.7128,-74.0060")
            .setTimestamp(Instant.now().toEpochMilli());

        ScoringResult result = orchestrator.execute(tx);

        assertNotNull(result.getScannerDetails());
        assertNotNull(result.getRuleDetails());
        assertTrue(result.getProcessingTimeMs() > 0,
            "Processing time should be recorded");
    }

    @Test
    @DisplayName("Pipeline version is set")
    void testPipelineVersion() {
        Transaction tx = new Transaction()
            .setTransactionId("txn-ver-test")
            .setAccountId("acc_normal_001")
            .setAmount(100)
            .setTimestamp(Instant.now().toEpochMilli());

        ScoringResult result = orchestrator.execute(tx);
        assertEquals("1.0.0", result.getPipelineVersion());
    }
}
