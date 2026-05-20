package com.fraud.detection.cache;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cassandra L2 cache — stores pre-computed aggregated metrics (30-90d TTL).
 *
 * Metrics stored per account:
 *   - tx_count_30d, tx_count_7d
 *   - total_amount_30d, avg_amount_30d
 *   - amount_p99, amount_p95, amount_p50
 *   - geo_velocity_30d
 *   - merchant_diversity_30d
 *   - device_change_count_30d
 *   - night_tx_ratio_30d
 *
 * In POC mode: uses in-memory store seeded with dummy data.
 */
@ApplicationScoped
public class CassandraService {

    private static final Logger log = LoggerFactory.getLogger(CassandraService.class);

    private boolean useLocal = true;
    private final Map<String, Map<String, Double>> localStore = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        try {
            String contactPoint = System.getenv().getOrDefault("CASSANDRA_HOST", "localhost");
            int port = Integer.parseInt(System.getenv().getOrDefault("CASSANDRA_PORT", "9042"));

            // In production: initialize Cassandra driver session
            // CqlSession session = CqlSession.builder()
            //     .addContactPoint(new InetSocketAddress(contactPoint, port))
            //     .withLocalDatacenter("datacenter1")
            //     .build();

            useLocal = true; // POC: always use local
            log.info("Cassandra not connected (POC mode), using in-memory metrics store");
        } catch (Exception e) {
            log.warn("Cassandra init failed: {}", e.getMessage());
        }

        // Seed with dummy metrics
        seedDummyData();
    }

    /** Get aggregated metrics for an account. */
    public Map<String, Double> getAccountMetrics(String accountId) {
        return localStore.getOrDefault(accountId, getDefaultMetrics());
    }

    /** Update metrics (called by nightly batch job). */
    public void updateMetrics(String accountId, Map<String, Double> metrics) {
        localStore.put(accountId, metrics);
    }

    private Map<String, Double> getDefaultMetrics() {
        Map<String, Double> m = new ConcurrentHashMap<>();
        m.put("tx_count_30d", 45.0);
        m.put("tx_count_7d", 12.0);
        m.put("total_amount_30d", 12500.0);
        m.put("avg_amount_30d", 277.0);
        m.put("amount_p99", 5000.0);
        m.put("amount_p95", 2000.0);
        m.put("amount_p50", 150.0);
        m.put("geo_velocity_30d", 50.0);
        m.put("merchant_diversity_30d", 8.0);
        m.put("device_change_count_30d", 1.0);
        m.put("night_tx_ratio_30d", 0.05);
        return m;
    }

    private void seedDummyData() {
        // Normal user
        Map<String, Double> normal = getDefaultMetrics();
        localStore.put("acc_normal_001", normal);

        // Suspicious user
        Map<String, Double> suspicious = getDefaultMetrics();
        suspicious.put("tx_count_30d", 200.0);
        suspicious.put("total_amount_30d", 150000.0);
        suspicious.put("amount_p99", 50000.0);
        suspicious.put("geo_velocity_30d", 800.0);
        suspicious.put("merchant_diversity_30d", 2.0);
        suspicious.put("device_change_count_30d", 5.0);
        suspicious.put("night_tx_ratio_30d", 0.6);
        localStore.put("acc_suspicious_001", suspicious);

        log.info("Seeded Cassandra dummy data for 2 accounts");
    }
}
