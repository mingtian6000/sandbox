package com.fraud.detection.model;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OnnxModelServiceTest {

    @Inject
    OnnxModelService modelService;

    @Test
    @DisplayName("ONNX model loads and returns score in [0, 100]")
    void testModelLoads() {
        FeatureVector fv = new FeatureVector()
            .setAmountNormalized(0.5)
            .setTxCount24h(5)
            .setAvgAmount24h(200)
            .setGeoVelocity(50)
            .setDeviceRiskScore(0.1)
            .setIpReputation(0.9)
            .setTimeAnomaly(0.05)
            .setAmountZscore(0.5)
            .setMerchantDiversity(8)
            .setNightTxRatio(0.02);

        double score = modelService.score(fv);

        System.out.println("Normal features → score: " + score);

        assertTrue(score >= 0 && score <= 100,
            "Score should be in [0, 100], got: " + score);
    }

    @Test
    @DisplayName("Fraud features produce higher score than normal features")
    void testFraudHigherThanNormal() {
        FeatureVector normal = new FeatureVector()
            .setAmountNormalized(0.1)
            .setTxCount24h(2)
            .setDeviceRiskScore(0.05)
            .setIpReputation(0.95)
            .setTimeAnomaly(0.01);

        FeatureVector fraud = new FeatureVector()
            .setAmountNormalized(5.0)
            .setTxCount24h(20)
            .setDeviceRiskScore(0.8)
            .setIpReputation(0.2)
            .setTimeAnomaly(0.8);

        double normalScore = modelService.score(normal);
        double fraudScore = modelService.score(fraud);

        System.out.println("Normal score: " + normalScore + ", Fraud score: " + fraudScore);

        assertTrue(fraudScore > normalScore,
            "Fraud features should produce higher score. Normal="
                + normalScore + ", Fraud=" + fraudScore);
    }

    @Test
    @DisplayName("Score scales with amount")
    void testAmountScaling() {
        FeatureVector lowAmount = new FeatureVector().setAmountNormalized(0.1);
        FeatureVector highAmount = new FeatureVector().setAmountNormalized(8.0);

        double lowScore = modelService.score(lowAmount);
        double highScore = modelService.score(highAmount);

        assertTrue(highScore >= lowScore,
            "Higher amount should produce >= score");
    }

    @Test
    @DisplayName("Model service is available or gracefully falls back")
    void testModelAvailability() {
        // Should not throw
        assertDoesNotThrow(() -> modelService.isAvailable());
    }
}
