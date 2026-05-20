package com.fraud.detection.resource;

import com.fraud.detection.model.ScoringResult;
import com.fraud.detection.model.Transaction;
import com.fraud.detection.pipeline.PipelineOrchestrator;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST API for fraud detection.
 *
 * POST /api/v1/fraud/score   — Score a single transaction
 * POST /api/v1/fraud/score/batch — Batch scoring
 * GET  /api/v1/fraud/health  — Health check
 */
@Path("/api/v1/fraud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FraudDetectionResource {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionResource.class);

    @Inject
    PipelineOrchestrator orchestrator;

    private final AtomicLong requestCounter = new AtomicLong(0);
    private final Map<String, Long> requestTimestamps = new ConcurrentHashMap<>();

    @POST
    @Path("/score")
    public Response scoreTransaction(Transaction tx) {
        long start = System.nanoTime();
        long reqId = requestCounter.incrementAndGet();

        if (tx.getTransactionId() == null || tx.getTransactionId().isBlank()) {
            tx.setTransactionId("txn-" + reqId + "-" + System.currentTimeMillis());
        }
        if (tx.getAccountId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "accountId is required"))
                .build();
        }

        log.info("[{}] Scoring transaction: account={}, amount={}",
            tx.getTransactionId(), tx.getAccountId(), tx.getAmount());

        try {
            ScoringResult result = orchestrator.execute(tx);
            long elapsed = (System.nanoTime() - start) / 1_000_000;

            log.info("[{}] Score={}, decision={}, time={}ms",
                tx.getTransactionId(), result.getFraudScore(),
                result.getDecision(), elapsed);

            return Response.ok(result).build();
        } catch (Exception e) {
            log.error("[{}] Pipeline failed: {}", tx.getTransactionId(), e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of(
                    "transactionId", tx.getTransactionId(),
                    "error", "Scoring failed: " + e.getMessage(),
                    "decision", "PENDING"
                ))
                .build();
        }
    }

    @POST
    @Path("/score/batch")
    public Response scoreBatch(java.util.List<Transaction> transactions) {
        log.info("Batch scoring {} transactions", transactions.size());
        var results = transactions.stream()
            .map(tx -> {
                try {
                    return orchestrator.execute(tx);
                } catch (Exception e) {
                    return ScoringResult.create(tx.getTransactionId())
                        .withFraudScore(50)
                        .withDecision("PENDING")
                        .withDecisionReason("Error: " + e.getMessage());
                }
            })
            .toList();
        return Response.ok(results).build();
    }

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "requestsServed", requestCounter.get(),
            "pipelineVersion", "1.0.0",
            "timestamp", System.currentTimeMillis()
        )).build();
    }
}
