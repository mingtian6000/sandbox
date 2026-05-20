package com.fraud.detection.scanner;

import com.fraud.detection.model.ScannerResult;
import com.fraud.detection.model.Transaction;
import com.fraud.detection.pipeline.PipelineContext;
import io.quarkus.arc.All;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Base interface for all scanners.
 * Scanners run in parallel during the pipeline.
 * Implementations are auto-discovered by Arc and injected into PipelineOrchestrator.
 */
public interface Scanner {
    String getName();
    ScannerResult scan(Transaction tx, PipelineContext ctx);
}
