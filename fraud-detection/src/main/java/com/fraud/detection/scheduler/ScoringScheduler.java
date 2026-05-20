package com.fraud.detection.scheduler;

import com.fraud.detection.cache.RedisService;
import com.fraud.detection.feature.FeatureExtractor;
import com.fraud.detection.model.Transaction;
import com.fraud.detection.model.ScoringResult;
import com.fraud.detection.pipeline.PipelineOrchestrator;
import com.fraud.detection.resource.FraudDetectionResource;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 批处理评分调度器 — 每 N 秒运行一次。
 *
 * 流程：
 *   ① 从 Redis 拉取待评分交易 ID 列表（drainPending）
 *   ② 遍历每个交易，重新构建 Transaction 对象
 *   ③ 执行完整 Pipeline（Scanner → Rules → Model → Decision）
 *   ④ 写回评分结果到 Redis
 *   ⑤ 更新统计信息
 *
 * 为什么用每秒批次代替实时评分？
 *   - 每秒 1000 笔交易，每笔都实时推理会打满 CPU
 *   - 批次处理可以攒批，GPU Batch Inference 提升吞吐 10x
 *   - 滑动窗口特征（24h 计数）在写入时已更新，评分时 O(1) 读取
 *   - 欺诈检测允许秒级延迟，不需要毫秒级实时
 */
@ApplicationScoped
public class ScoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScoringScheduler.class);

    @Inject
    RedisService redisService;

    @Inject
    FeatureExtractor featureExtractor;

    @Inject
    PipelineOrchestrator orchestrator;

    @Inject
    FraudDetectionResource resource;

    @ConfigProperty(name = "fraud.batch.interval-seconds", defaultValue = "60")
    int intervalSeconds;

    @ConfigProperty(name = "fraud.batch.max-batch-size", defaultValue = "5000")
    int maxBatchSize;

    /**
     * 每 intervalSeconds 秒执行一次批次评分。
     */
    @Scheduled(every = "{fraud.batch.interval-seconds}s", identity = "scoring-batch")
    void batchScore() {
        long start = System.currentTimeMillis();
        log.info("═══ Batch scoring cycle starting ═══");

        try {
            // ① 从 Redis 拉取待评分队列
            List<String> pendingIds = redisService.drainPending(maxBatchSize);
            if (pendingIds.isEmpty()) {
                log.debug("No pending transactions to score");
                return;
            }

            log.info("Processing batch of {} transactions", pendingIds.size());

            // ②-④ 逐笔评分
            int scored = 0;
            for (String txnId : pendingIds) {
                try {
                    // 从 Redis 重建 Transaction（POC 简化版）
                    Transaction tx = rebuildTransaction(txnId);
                    if (tx == null) continue;

                    ScoringResult result = orchestrator.execute(tx);

                    // 写回评分结果
                    redisService.markScored(txnId, result.getFraudScore(), result.getDecision());

                    if (scored < 3) { // 只打前 3 笔的日志
                        log.info("  [{}/{}] score={}, decision={}, {}ms",
                            scored + 1, pendingIds.size(),
                            String.format("%.1f", result.getFraudScore()),
                            result.getDecision(),
                            result.getProcessingTimeMs());
                    }

                    scored++;
                } catch (Exception e) {
                    log.warn("  [{}] scoring failed: {}", txnId, e.getMessage());
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            resource.recordBatch(scored);

            log.info("═══ Batch done: {}/{} scored in {}ms ═══",
                scored, pendingIds.size(), elapsed);

        } catch (Exception e) {
            log.error("Batch scoring cycle failed", e);
        }
    }

    /**
     * POC: 从 Redis 原始交易数据重建 Transaction。
     * 生产环境直接从 Redis 反序列化 JSON。
     */
    private Transaction rebuildTransaction(String txnId) {
        // POC: 构造一个最小化的 Transaction 对象
        // 生产环境从 Redis 反序列化完整 JSON
        Transaction tx = new Transaction();
        tx.setTransactionId(txnId);
        tx.setAccountId("acc_normal_001");  // POC 默认
        tx.setAmount(100.0);
        tx.setTimestamp(System.currentTimeMillis());
        return tx;
    }
}
