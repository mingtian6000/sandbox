package com.fraud.detection.decision;

import com.fraud.detection.pipeline.PipelineContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Makes the final decision based on fraud score.
 *
 * Thresholds (configurable):
 *   < 30  → APPROVE
 *   30-60 → PENDING (manual review)
 *   > 60  → REJECT
 */
@ApplicationScoped
public class DecisionMaker {

    /** Thresholds — can be injected from config. */
    static final double THRESHOLD_APPROVE = 30;
    static final double THRESHOLD_REJECT  = 60;

    public enum Decision {
        APPROVE("Transaction approved — low risk"),
        REJECT("Transaction rejected — high risk"),
        PENDING("Requires manual review — medium risk");

        private final String reason;
        Decision(String reason) { this.reason = reason; }
        public String getReason() { return reason; }
    }

    /**
     * Decide on a transaction based on its fraud score.
     * Also considers pipeline warnings (circuit breaker).
     */
    public Decision decide(double fraudScore, PipelineContext ctx) {
        // If pipeline had critical errors, escalate
        if (ctx.hasWarnings() && fraudScore >= 30) {
            return Decision.PENDING;
        }

        if (fraudScore >= THRESHOLD_REJECT) {
            return Decision.REJECT;
        } else if (fraudScore >= THRESHOLD_APPROVE) {
            return Decision.PENDING;
        } else {
            return Decision.APPROVE;
        }
    }
}
