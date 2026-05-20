package com.fraud.detection.model;

/**
 * Result from a single Scanner module.
 */
public class ScannerResult {
    private String scannerName;
    private double riskScore;       // 0-1
    private String riskLevel;       // LOW / MEDIUM / HIGH
    private String reasonCode;
    private String detail;

    public ScannerResult() {}

    public static ScannerResult create(String name) {
        ScannerResult r = new ScannerResult();
        r.scannerName = name;
        return r;
    }

    public ScannerResult withRiskScore(double s)    { this.riskScore = s; return this; }
    public ScannerResult withRiskLevel(String l)    { this.riskLevel = l; return this; }
    public ScannerResult withReasonCode(String c)   { this.reasonCode = c; return this; }
    public ScannerResult withDetail(String d)       { this.detail = d; return this; }

    public String getScannerName()  { return scannerName; }
    public double getRiskScore()    { return riskScore; }
    public String getRiskLevel()    { return riskLevel; }
    public String getReasonCode()   { return reasonCode; }
    public String getDetail()       { return detail; }
}
