package com.fraud.detection.scanner;

import com.fraud.detection.model.ScannerResult;
import com.fraud.detection.model.Transaction;
import com.fraud.detection.pipeline.PipelineContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Scans for device fingerprint anomalies.
 * Checks: new device for account, device linked to fraud history,
 *         emulator/virtual device indicators.
 */
@ApplicationScoped
public class DeviceFingerprintScanner implements Scanner {

    @Override
    public String getName() { return "device_fingerprint"; }

    @Override
    public ScannerResult scan(Transaction tx, PipelineContext ctx) {
        ScannerResult result = ScannerResult.create(getName());
        String deviceId = tx.getDeviceId();
        if (deviceId == null || deviceId.isBlank()) {
            return result.withRiskScore(0.3)
                .withRiskLevel("MEDIUM")
                .withReasonCode("NO_DEVICE_ID")
                .withDetail("Transaction missing device identifier");
        }

        // Check for known virtual/emulator device patterns
        if (deviceId.startsWith("emulator-") || deviceId.startsWith("genymotion-")) {
            return result.withRiskScore(0.7)
                .withRiskLevel("HIGH")
                .withReasonCode("EMULATOR_DETECTED")
                .withDetail("Transaction from virtual device: " + deviceId);
        }

        // Check device velocity — same device used across many accounts recently
        // (In real impl, query Redis for device-to-account mapping)
        Integer recentAccountCount = ctx.getAttribute("device_account_count_24h");
        if (recentAccountCount != null && recentAccountCount > 5) {
            return result.withRiskScore(0.6)
                .withRiskLevel("HIGH")
                .withReasonCode("DEVICE_ACCOUNT_FARMING")
                .withDetail("Device used with " + recentAccountCount + " accounts in 24h");
        }

        // New device for a known account
        Boolean isNewDevice = ctx.getAttribute("is_new_device");
        if (Boolean.TRUE.equals(isNewDevice)) {
            return result.withRiskScore(0.4)
                .withRiskLevel("MEDIUM")
                .withReasonCode("NEW_DEVICE")
                .withDetail("Device not previously seen for this account");
        }

        return result.withRiskScore(0.05)
            .withRiskLevel("LOW")
            .withReasonCode("CLEAN")
            .withDetail("Device fingerprint normal");
    }
}
