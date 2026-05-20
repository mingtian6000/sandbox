package com.fraud.detection.cache;

import com.fraud.detection.model.Transaction;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis L1 cache — stores raw transaction data (24h TTL).
 *
 * Data stored:
 *   Key: "txn:recent:{accountId}"  →  List of recent transaction JSON
 *   Key: "txn:device:{deviceId}"   →  Set of account IDs using this device
 *   Key: "txn:ip:{ipAddress}"      →  Set of account IDs using this IP
 *
 * TTL: 24 hours (expired automatically)
 */
@ApplicationScoped
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);

    // In POC mode, use in-memory store as fallback
    private final java.util.Map<String, List<Transaction>> localStore = new java.util.concurrent.ConcurrentHashMap<>();
    private boolean useLocal = true;
    private JedisPool pool;

    @PostConstruct
    void init() {
        try {
            String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
            int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
            var config = new JedisPoolConfig();
            config.setMaxTotal(32);
            config.setMaxIdle(8);
            config.setMinIdle(2);
            pool = new JedisPool(config, host, port, 2000);
            // Test connection
            try (var jedis = pool.getResource()) {
                jedis.ping();
                useLocal = false;
                log.info("Redis connected: {}:{}", host, port);
            }
        } catch (Exception e) {
            log.warn("Redis not available, using local in-memory fallback: {}", e.getMessage());
            useLocal = true;
        }
    }

    /** Get recent transactions for an account. */
    public List<Transaction> getRecentTransactions(String accountId, int limit) {
        if (useLocal) {
            List<Transaction> txns = localStore.getOrDefault(accountId, List.of());
            return txns.size() > limit ? txns.subList(0, limit) : txns;
        }

        try (var jedis = pool.getResource()) {
            String key = "txn:recent:" + accountId;
            var jsonList = jedis.lrange(key, 0, limit - 1);
            // In production: deserialize JSON → Transaction
            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("Redis read failed: {}", e.getMessage());
            return List.of();
        }
    }

    /** Store a transaction in the recent list. */
    public void storeTransaction(Transaction tx) {
        if (useLocal) {
            localStore.computeIfAbsent(tx.getAccountId(), k -> new ArrayList<>()).add(tx);
            return;
        }
        try (var jedis = pool.getResource()) {
            String key = "txn:recent:" + tx.getAccountId();
            // jedis.lpush(key, serialize(tx));
            // jedis.ltrim(key, 0, 999);
            // jedis.expire(key, 86400); // 24h TTL
        } catch (Exception e) {
            log.warn("Redis write failed: {}", e.getMessage());
        }
    }

    /** Get device to account mapping. */
    public int getDeviceAccountCount(String deviceId) {
        if (useLocal) return 0;
        try (var jedis = pool.getResource()) {
            String key = "txn:device:" + deviceId;
            var members = jedis.smembers(key);
            return members.size();
        } catch (Exception e) {
            return 0;
        }
    }
}
