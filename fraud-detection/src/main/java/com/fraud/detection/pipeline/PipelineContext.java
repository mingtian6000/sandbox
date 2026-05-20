package com.fraud.detection.pipeline;

import com.fraud.detection.model.Transaction;

/**
 * Context shared across all pipeline stages.
 * Carries transaction data, extracted features, and accumulated warnings.
 */
public class PipelineContext {
    private final Transaction transaction;
    private final java.util.Map<String, Object> attributes = new java.util.HashMap<>();
    private final java.util.Map<String, String> warnings = new java.util.LinkedHashMap<>();
    private com.fraud.detection.model.FeatureVector features;

    public PipelineContext(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransaction() { return transaction; }

    public com.fraud.detection.model.FeatureVector getFeatures() { return features; }
    public void setFeatures(com.fraud.detection.model.FeatureVector features) { this.features = features; }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attributes.get(key); }
    public void setAttribute(String key, Object value) { attributes.put(key, value); }

    public void addWarning(String stage, String message) { warnings.put(stage, message); }
    public java.util.Map<String, String> getWarnings() { return warnings; }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
}
