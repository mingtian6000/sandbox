package com.fraud.detection.resource;

import com.fraud.detection.batch.BatchReport;
import com.fraud.detection.batch.NightlyBatchJob;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 管理端点 — 手动触发夜间 Batch / 查看上一次 Batch 报告。
 */
@Path("/api/v1/admin/batch")
@Produces(MediaType.APPLICATION_JSON)
public class BatchResource {

    private static final Logger log = LoggerFactory.getLogger(BatchResource.class);

    @Inject
    NightlyBatchJob nightlyBatchJob;

    /**
     * 手动触发夜间 Batch。
     * 用于测试 / 运维手动补跑。
     */
    @POST
    @Path("/run")
    public Response triggerBatch() {
        log.info("Manual trigger: nightly batch job");
        try {
            nightlyBatchJob.runNightlyBatch();
            BatchReport report = nightlyBatchJob.getLastReport();
            return Response.ok(Map.of(
                "status", "COMPLETED",
                "elapsedMs", report != null ? report.getElapsedMs() : 0,
                "accountsProcessed", report != null ? report.getAccountsProcessed() : 0,
                "accountsUpdated", report != null ? report.getAccountsUpdated() : 0,
                "transactionsScanned", report != null ? report.getTotalTransactionsScanned() : 0
            )).build();
        } catch (Exception e) {
            log.error("Manual batch trigger failed", e);
            return Response.serverError()
                .entity(Map.of("status", "FAILED", "error", e.getMessage()))
                .build();
        }
    }

    /**
     * 查看上一次 Batch 运行报告。
     */
    @GET
    @Path("/last-report")
    public Response getLastReport() {
        BatchReport report = nightlyBatchJob.getLastReport();
        if (report == null || report.getAccountsProcessed() == 0) {
            return Response.ok(Map.of(
                "status", "NO_REPORT",
                "message", "Nightly batch has not run yet. Trigger with POST /api/v1/admin/batch/run"
            )).build();
        }

        return Response.ok(Map.of(
            "status", "AVAILABLE",
            "accountsProcessed", report.getAccountsProcessed(),
            "accountsUpdated", report.getAccountsUpdated(),
            "transactionsScanned", report.getTotalTransactionsScanned(),
            "elapsedMs", report.getElapsedMs(),
            "details", report.getSummaries().stream().limit(10).map(s -> Map.of(
                "accountId", s.accountId,
                "transactions", s.txnCount,
                "metricsBefore", s.metricsBefore,
                "metricsAfter", s.metricsAfter
            )).toList()
        )).build();
    }
}
