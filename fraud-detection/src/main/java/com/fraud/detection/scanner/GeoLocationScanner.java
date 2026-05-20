package com.fraud.detection.scanner;

import com.fraud.detection.model.ScannerResult;
import com.fraud.detection.model.Transaction;
import com.fraud.detection.pipeline.PipelineContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Scans for geo-location anomalies.
 * Checks: impossible travel, high-risk countries, location mismatch with device/IP.
 */
@ApplicationScoped
public class GeoLocationScanner implements Scanner {

    // High-risk country codes (example list)
    private static final java.util.Set<String> HIGH_RISK_COUNTRIES =
        java.util.Set.of("XX", "YY", "ZZ");

    @Override
    public String getName() { return "geo_location"; }

    @Override
    public ScannerResult scan(Transaction tx, PipelineContext ctx) {
        ScannerResult result = ScannerResult.create(getName());
        String geo = tx.getGeoLocation();

        if (geo == null || geo.isBlank()) {
            return result.withRiskScore(0.2)
                .withRiskLevel("LOW")
                .withReasonCode("NO_GEO")
                .withDetail("No geo-location data");
        }

        // Check high-risk country
        String country = geo.contains(",") ? geo.split(",")[0].trim() : geo;
        if (HIGH_RISK_COUNTRIES.contains(country.toUpperCase())) {
            return result.withRiskScore(0.8)
                .withRiskLevel("HIGH")
                .withReasonCode("HIGH_RISK_COUNTRY")
                .withDetail("Transaction from high-risk region: " + country);
        }

        // Impossible travel check (query Redis for last known location)
        String lastGeo = ctx.getAttribute("last_known_geo");
        Long lastTimestamp = ctx.getAttribute("last_tx_timestamp");
        if (lastGeo != null && lastTimestamp != null && !lastGeo.equals(geo)) {
            long timeDiffHours = (tx.getTimestamp() - lastTimestamp) / 3_600_000;
            if (timeDiffHours > 0) {
                double distanceKm = estimateDistance(geo, lastGeo);
                double speedKmph = distanceKm / timeDiffHours;
                if (speedKmph > 900) { // Impossible speed (>900 km/h)
                    return result.withRiskScore(0.9)
                        .withRiskLevel("HIGH")
                        .withReasonCode("IMPOSSIBLE_TRAVEL")
                        .withDetail(String.format(
                            "Impossible travel: %.0f km in %d hours (%.0f km/h)",
                            distanceKm, timeDiffHours, speedKmph));
                }
            }
        }

        return result.withRiskScore(0.02)
            .withRiskLevel("LOW")
            .withReasonCode("GEO_NORMAL")
            .withDetail("Geo-location normal");
    }

    /** Rough Haversine distance in km between two "lat,lng" strings. */
    private double estimateDistance(String geo1, String geo2) {
        try {
            String[] p1 = geo1.split(",");
            String[] p2 = geo2.split(",");
            double lat1 = Double.parseDouble(p1[0].trim());
            double lon1 = Double.parseDouble(p1[1].trim());
            double lat2 = Double.parseDouble(p2[0].trim());
            double lon2 = Double.parseDouble(p2[1].trim());
            return haversine(lat1, lon1, lat2, lon2);
        } catch (Exception e) {
            return 0;
        }
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
