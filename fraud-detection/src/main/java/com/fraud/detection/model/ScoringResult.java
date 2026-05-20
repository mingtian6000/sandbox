package com.fraud.detection.model;

import java.util.List;

/**
 * Final scoring result returned by the pipeline.
 */
public class ScoringResult {
    private String transactionId;
    private double fraudScore;       // 0-100
    private String decision;         // APPROVE / REJECT / PENDING
    private String decisionReason;
    private double modelScore;
    private double rulesScore;
    private double scannersScore;
    private List<ScannerResult> scannerDetails;
    private List<RuleResult> ruleDetails;
    private long processingTimeMs;
    private String pipelineVersion;

    public ScoringResult() {}

    // --- builder ---
    public static ScoringResult create(String transactionId) {
        ScoringResult r = new ScoringResult();
        r.transactionId = transactionId;
        return r;
    }

    public ScoringResult withFraudScore(double score)    { this.fraudScore = score; return this; }
    public ScoringResult withDecision(String decision)    { this.decision = decision; return this; }
    public ScoringResult withDecisionReason(String reason){ this.decisionReason = reason; return this; }
    public ScoringResult withModelScore(double s)         { this.modelScore = s; return this; }
    public ScoringResult withRulesScore(double s)         { this.rulesScore = s; return this; }
    public ScoringResult withScannersScore(double s)      { this.scannersScore = s; return this; }
    public ScoringResult withScannerDetails(List<ScannerResult> d) { this.scannerDetails = d; return this; }
    public ScoringResult withRuleDetails(List<RuleResult> d)       { this.ruleDetails = d; return this; }
    public ScoringResult withProcessingTime(long ms)      { this.processingTimeMs = ms; return this; }
    public ScoringResult withPipelineVersion(String v)    { this.pipelineVersion = v; return this; }

    // --- getters ---
    public String getTransactionId()     { return transactionId; }
    public double getFraudScore()        { return fraudScore; }
    public String getDecision()          { return decision; }
    public String getDecisionReason()    { return decisionReason; }
    public double getModelScore()        { return modelScore; }
    public double getRulesScore()        { return rulesScore; }
    public double getScannersScore()     { return scannersScore; }
    public List<ScannerResult> getScannerDetails() { return scannerDetails; }
    public List<RuleResult> getRuleDetails()        { return ruleDetails; }
    public long getProcessingTimeMs()    { return processingTimeMs; }
    public String getPipelineVersion()   { return pipelineVersion; }
}
