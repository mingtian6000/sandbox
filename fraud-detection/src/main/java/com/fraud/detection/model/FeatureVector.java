package com.fraud.detection.model;

import java.util.Arrays;

/**
 * Feature vector extracted from raw + aggregated data.
 * Input to the ONNX model.
 */
public class FeatureVector {
    public static final int FEATURE_COUNT = 10;

    // Index mapping for ONNX model input
    public static final int IDX_AMOUNT_NORMALIZED   = 0;
    public static final int IDX_TX_COUNT_24H        = 1;
    public static final int IDX_AVG_AMOUNT_24H      = 2;
    public static final int IDX_GEO_VELOCITY        = 3;
    public static final int IDX_DEVICE_RISK_SCORE   = 4;
    public static final int IDX_IP_REPUTATION       = 5;
    public static final int IDX_TIME_ANOMALY        = 6;
    public static final int IDX_AMOUNT_ZSCORE       = 7;
    public static final int IDX_MERCHANT_DIVERSITY  = 8;
    public static final int IDX_NIGHT_TX_RATIO      = 9;

    private final double[] features = new double[FEATURE_COUNT];

    public FeatureVector() {}

    public FeatureVector set(int index, double value) {
        if (index >= 0 && index < FEATURE_COUNT) features[index] = value;
        return this;
    }

    public double get(int index) { return features[index]; }

    /** Convert to ONNX-friendly float array (batch=1). */
    public float[] toFloatArray() {
        float[] result = new float[FEATURE_COUNT];
        for (int i = 0; i < FEATURE_COUNT; i++) {
            result[i] = (float) features[i];
        }
        return result;
    }

    // ---- convenience setters ----
    public FeatureVector setAmountNormalized(double v)   { return set(IDX_AMOUNT_NORMALIZED, v); }
    public FeatureVector setTxCount24h(int v)            { return set(IDX_TX_COUNT_24H, v); }
    public FeatureVector setAvgAmount24h(double v)       { return set(IDX_AVG_AMOUNT_24H, v); }
    public FeatureVector setGeoVelocity(double v)        { return set(IDX_GEO_VELOCITY, v); }
    public FeatureVector setDeviceRiskScore(double v)    { return set(IDX_DEVICE_RISK_SCORE, v); }
    public FeatureVector setIpReputation(double v)       { return set(IDX_IP_REPUTATION, v); }
    public FeatureVector setTimeAnomaly(double v)        { return set(IDX_TIME_ANOMALY, v); }
    public FeatureVector setAmountZscore(double v)       { return set(IDX_AMOUNT_ZSCORE, v); }
    public FeatureVector setMerchantDiversity(int v)     { return set(IDX_MERCHANT_DIVERSITY, v); }
    public FeatureVector setNightTxRatio(double v)       { return set(IDX_NIGHT_TX_RATIO, v); }

    public double getAmountNormalized()  { return get(IDX_AMOUNT_NORMALIZED); }
    public double getTxCount24h()        { return get(IDX_TX_COUNT_24H); }
    public double getAvgAmount24h()      { return get(IDX_AVG_AMOUNT_24H); }
    public double getGeoVelocity()       { return get(IDX_GEO_VELOCITY); }
    public double getDeviceRiskScore()   { return get(IDX_DEVICE_RISK_SCORE); }
    public double getIpReputation()      { return get(IDX_IP_REPUTATION); }
    public double getTimeAnomaly()       { return get(IDX_TIME_ANOMALY); }
    public double getAmountZscore()      { return get(IDX_AMOUNT_ZSCORE); }
    public double getMerchantDiversity() { return get(IDX_MERCHANT_DIVERSITY); }
    public double getNightTxRatio()      { return get(IDX_NIGHT_TX_RATIO); }

    @Override
    public String toString() {
        return "FeatureVector" + Arrays.toString(features);
    }
}
