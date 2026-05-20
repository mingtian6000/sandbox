package com.fraud.detection.cache;

import com.fraud.detection.model.Transaction;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Redis — always-updated sliding window.
 *
 * Stores TWO kinds of data, both updated on EVERY transaction:
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │  ① 原始交易（24h 滑动窗口）                              │
 * │  LPUSH txn:recent:{accountId} <json>                    │
 * │  LTRIM 0 999 + EXPIRE 86400                             │
 * ├─────────────────────────────────────────────────────────┤
 * │  ② Feature Counters（随每笔交易增量更新）                 │
 * │  INCR  feat:{accountId}:tx_count_24h                    │
 * │  INCRBYFLOAT  feat:{accountId}:total_amount_24h  <amt>  │
 * │  SADD  feat:{accountId}:merchants <merchantId>          │
 * │  SADD  feat:{accountId}:devices  <deviceId>             │
 * │  ... (all with TTL 86400)                               │
 * ├─────────────────────────────────────────────────────────┤
 * │  ③ 待评分队列                                           │
 * │  LPUSH txn:pending <txnId>                              │
 * └─────────────────────────────────────────────────────────┘
 *
 * ScoringScheduler 每 60s 捞取 txn:pending → 批量评分。
 * 评分时直接读 feat:* 预计算值，无需遍历原始交易。
 */
@ApplicationScoped
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);
    private static final long TTL_24H = 86400;
    private static final int MAX_RECENT = 1000;
    private static final String FEAT_PREFIX = "feat:";
    private static final String RECENT_PREFIX = "txn:recent:";
    private static final String PENDING_KEY = "txn:pending";
    private static final String DEVICE_PREFIX = "txn:device:";

    // POC in-memory stores
    private final Map<String, List<Transaction>> recentStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> featureStore = new ConcurrentHashMap<>();
    private final Queue<String> pendingQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, Set<String>> deviceAccountMap = new ConcurrentHashMap<>();

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
            try (var jedis = pool.getResource()) {
                jedis.ping();
                useLocal = false;
                log.info("✅ Redis connected: {}:{}", host, port);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, in-memory fallback: {}", e.getMessage());
            useLocal = true;
        }
    }

    // ====================================================================
    //  核心写入 — 每笔交易必调，同步更新原始数据 + 特征计数器
    // ====================================================================

    /** 写入一笔交易：原始数据 + 特征计数器 + 待评分队列，一步到位。 */
    public void recordTransaction(Transaction tx) {
        String aid = tx.getAccountId();
        String txnJson = serialize(tx);

        if (useLocal) {
            appendRecent(aid, tx);
            updateFeaturesLocal(aid, tx);
            pendingQueue.add(tx.getTransactionId());
            deviceAccountMap.computeIfAbsent(tx.getDeviceId(), k -> ConcurrentHashMap.newKeySet()).add(aid);
            return;
        }

        try (var jedis = pool.getResource()) {
            var p = jedis.pipelined();

            // ① 原始交易滑动窗口
            p.lpush(RECENT_PREFIX + aid, txnJson);
            p.ltrim(RECENT_PREFIX + aid, 0, MAX_RECENT - 1);
            p.expire(RECENT_PREFIX + aid, TTL_24H);

            // ② 特征计数器（增量更新 — 始终最新）
            String f = FEAT_PREFIX + aid;
            p.hincrBy(f, "tx_count_24h", 1);
            p.hincrByFloat(f, "total_amount_24h", tx.getAmount());
            p.hincrBy(f, "merchant_count", 1);
            p.sadd(f + ":merchants", tx.getMerchantId());
            p.expire(f, TTL_24H);
            p.expire(f + ":merchants", TTL_24H);

            // ③ 设备映射
            if (tx.getDeviceId() != null) {
                p.sadd(DEVICE_PREFIX + tx.getDeviceId(), aid);
                p.expire(DEVICE_PREFIX + tx.getDeviceId(), TTL_24H);
            }

            // ④ 待评分队列
            p.lpush(PENDING_KEY, tx.getTransactionId());
            p.expire(PENDING_KEY, 3600);

            p.sync();
        } catch (Exception e) {
            log.warn("Redis write failed: {}", e.getMessage());
        }
    }

    // ====================================================================
    //  读取预计算特征 — 评分时直接读，O(1)
    // ====================================================================

    /** 读 Redis 中的实时特征，回退到默认值。 */
    public Map<String, String> getFeatures(String accountId) {
        if (useLocal) {
            return featureStore.getOrDefault(accountId, Map.of());
        }
        try (var jedis = pool.getResource()) {
            Map<String, String> feat = jedis.hgetAll(FEAT_PREFIX + accountId);
            return feat.isEmpty() ? Map.of() : feat;
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** 获取有活动数据的账户 ID 列表。 */
    public Set<String> getActiveAccounts() {
        if (useLocal) {
            Set<String> all = new HashSet<>(recentStore.keySet());
            all.addAll(featureStore.keySet());
            return all;
        }
        try (var jedis = pool.getResource()) {
            // Redis: scan keys matching "txn:recent:*"
            Set<String> accounts = new HashSet<>();
            String cursor = "0";
            do {
                var scanResult = jedis.scan(cursor,
                    new redis.clients.jedis.params.ScanParams().match("txn:recent:*").count(1000));
                for (String key : scanResult.getResult()) {
                    accounts.add(key.replace("txn:recent:", ""));
                }
                cursor = scanResult.getCursor();
            } while (!cursor.equals("0"));
            return accounts;
        } catch (Exception e) {
            log.warn("getActiveAccounts failed: {}", e.getMessage());
            return Set.of();
        }
    }

    /** 读设备关联账户数。 */
    public int getDeviceAccountCount(String deviceId) {
        if (useLocal || deviceId == null) return 0;
        try (var jedis = pool.getResource()) {
            return (int) jedis.scard(DEVICE_PREFIX + deviceId);
        } catch (Exception e) { return 0; }
    }

    // ====================================================================
    //  待评分队列 — ScoringScheduler 每 60s 调用
    // ====================================================================

    /** 拉取并清空待评分队列（原子操作）。 */
    public List<String> drainPending(int maxBatch) {
        if (useLocal) {
            List<String> batch = new ArrayList<>();
            String id;
            while ((id = pendingQueue.poll()) != null && batch.size() < maxBatch) batch.add(id);
            return batch;
        }
        try (var jedis = pool.getResource()) {
            List<String> all = jedis.lrange(PENDING_KEY, 0, maxBatch - 1);
            if (!all.isEmpty()) jedis.del(PENDING_KEY);
            return all;
        } catch (Exception e) {
            log.warn("drainPending failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ====================================================================
    //  辅助
    // ====================================================================

    public List<Transaction> getRecentTransactions(String accountId, int limit) {
        if (useLocal) {
            List<Transaction> list = recentStore.getOrDefault(accountId, List.of());
            return list.size() > limit ? list.subList(0, limit) : list;
        }
        return List.of();
    }

    public void markScored(String txnId, double score, String decision) {
        // 可选：记录评分结果到 Redis
    }

    // ====================================================================
    //  POC 本地实现
    // ====================================================================

    private void appendRecent(String aid, Transaction tx) {
        recentStore.compute(aid, (k, list) -> {
            if (list == null) list = new ArrayList<>();
            list.add(0, tx);
            if (list.size() > MAX_RECENT) list.remove(list.size() - 1);
            return list;
        });
    }

    private void updateFeaturesLocal(String aid, Transaction tx) {
        featureStore.compute(aid, (k, feat) -> {
            if (feat == null) feat = new ConcurrentHashMap<>();
            feat.put("tx_count_24h", String.valueOf(Integer.parseInt(feat.getOrDefault("tx_count_24h", "0")) + 1));
            double total = Double.parseDouble(feat.getOrDefault("total_amount_24h", "0")) + tx.getAmount();
            feat.put("total_amount_24h", String.valueOf(total));
            feat.put("merchant_count", String.valueOf(Integer.parseInt(feat.getOrDefault("merchant_count", "0")) + 1));
            return feat;
        });
    }

    private String serialize(Transaction tx) {
        return String.format(
            "{\"txnId\":\"%s\",\"account\":\"%s\",\"amount\":%.2f,\"ts\":%d}",
            tx.getTransactionId(), tx.getAccountId(), tx.getAmount(), tx.getTimestamp());
    }
}
