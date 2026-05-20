package com.fraud.detection.decision;

import com.fraud.detection.pipeline.PipelineContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class DecisionMakerTest {

    @Inject
    DecisionMaker decisionMaker;

    @ParameterizedTest
    @CsvSource({
        "0,    APPROVE",
        "15,   APPROVE",
        "29.9, APPROVE",
        "30,   PENDING",
        "45,   PENDING",
        "59.9, PENDING",
        "60,   REJECT",
        "80,   REJECT",
        "100,  REJECT",
    })
    @DisplayName("Decision threshold mapping is correct")
    void testThresholds(double score, String expected) {
        PipelineContext ctx = new PipelineContext(null);
        DecisionMaker.Decision decision = decisionMaker.decide(score, ctx);
        assertEquals(expected, decision.name(),
            "Score " + score + " should map to " + expected);
    }

    @Test
    @DisplayName("Pipeline warnings escalate medium scores to PENDING")
    void testWarningsEscalate() {
        PipelineContext ctx = new PipelineContext(null);
        ctx.addWarning("scanner1", "timeout");
        // Score 25 is normally APPROVE, but with warnings it stays PENDING
        DecisionMaker.Decision decision = decisionMaker.decide(25, ctx);
        assertNotEquals(DecisionMaker.Decision.REJECT, decision);
    }

    @Test
    @DisplayName("Low score with no warnings → APPROVE")
    void testCleanApprove() {
        PipelineContext ctx = new PipelineContext(null);
        assertEquals(DecisionMaker.Decision.APPROVE,
            decisionMaker.decide(10, ctx));
    }

    @Test
    @DisplayName("Decision reasons are descriptive")
    void testDecisionReason() {
        PipelineContext ctx = new PipelineContext(null);

        assertEquals("Transaction approved — low risk",
            decisionMaker.decide(10, ctx).getReason());
        assertEquals("Transaction rejected — high risk",
            decisionMaker.decide(80, ctx).getReason());
        assertEquals("Requires manual review — medium risk",
            decisionMaker.decide(45, ctx).getReason());
    }
}
