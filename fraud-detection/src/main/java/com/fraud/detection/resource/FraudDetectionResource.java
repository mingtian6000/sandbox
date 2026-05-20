package com.fraud.detection.resource;

import com.fraud.detection.cache.RedisService;
import com.fraud.detection.model.Transaction;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST API for fraud detection.
 *
 * POST /api/v1/fraud/transaction  — Receive transaction, write to Redis, return immediately
 * GET  /api/v1/fraud/health       — Health check
 * GET  /api/v1/fraud/stats        — Batch scoring stats
 *
 * Scoring happens asynchronously every 60s via ScoringScheduler.
 */
@Path("/api/v1/fraud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FraudDetectionResource {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionResource.class);

    @Inject
    RedisService redisService;

    private final AtomicLong requestCounter = new AtomicLong(0);
    private final AtomicLong batchCounter = new AtomicLong(0);
    private volatile long lastBatchTime = 0;
    private volatile long lastBatchCount = 0;

    /**
     * Receive a transaction, write to Redis sliding window, return immediately.
     * Scoring is handled asynchronously by ScoringScheduler (every 60s).
     */
    @POST
    @Path("/transaction")
    public Response receiveTransaction(Transaction tx) {
        long reqId = requestCounter.incrementAndGet();

        // Validate
        if (tx.getAccountId() == null || tx.getAccountId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "accountId is required"))
                .build();
        }

        // Assign ID if missing
        if (tx.getTransactionId() == null || tx.getTransactionId().isBlank()) {
            tx.setTransactionId("txn-" + reqId + "-" + System.currentTimeMillis());
        }
        if (tx.getTimestamp() <= 0) {
            tx.setTimestamp(System.currentTimeMillis());
        }

        log.info("[{}] Received txn: account={}, amount={}, device={}",
            tx.getTransactionId(), tx.getAccountId(), tx.getAmount(), tx.getDeviceId());

        // WRITE TO REDIS (always) — 24h sliding window
        redisService.recordTransaction(tx);

        // Return immediately — scoring happens async
        return Response.accepted(Map.of(
            "transactionId", tx.getTransactionId(),
            "status", "RECEIVED",
            "message", "Transaction received. Scoring will be processed in next batch cycle.",
            "estimatedProcessing", "~60s"
        )).build();
    }

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "requestsReceived", requestCounter.get(),
            "batchesProcessed", batchCounter.get(),
            "lastBatchCount", lastBatchCount,
            "lastBatchTime", lastBatchTime,
            "pipelineVersion", "1.0.0",
            "scoringMode", "BATCH_EVERY_60S",
            "timestamp", System.currentTimeMillis()
        )).build();
    }

    // Called by ScoringScheduler after each batch
    public void recordBatch(long count) {
        batchCounter.incrementAndGet();
        lastBatchTime = System.currentTimeMillis();
        lastBatchCount = count;
    }
}
