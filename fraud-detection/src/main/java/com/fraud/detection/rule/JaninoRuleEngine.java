package com.fraud.detection.rule;

import com.fraud.detection.model.FeatureVector;
import com.fraud.detection.model.RuleResult;
import com.fraud.detection.model.Transaction;
import com.fraud.detection.pipeline.PipelineContext;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codehaus.janino.ExpressionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic rules engine powered by Janino.
 *
 * Rules are defined in YAML configuration and compiled to Java bytecode
 * at startup. Expressions are evaluated in-memory with type safety,
 * no reflection overhead.
 *
 * Example rule expression:
 *   "amount > 10000 && geoVelocity > 500"
 *   Variables available: amount, txCount24h, geoVelocity, deviceRiskScore,
 *                        ipReputation, timeAnomaly, amountZscore, ...
 */
@ApplicationScoped
public class JaninoRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(JaninoRuleEngine.class);

    @Inject
    RuleConfigLoader configLoader;

    /** Compiled evaluators cache: ruleId → ExpressionEvaluator */
    private final Map<String, ExpressionEvaluator> evaluatorCache = new ConcurrentHashMap<>();

    private List<FraudRule> rules;

    @PostConstruct
    void init() {
        this.rules = configLoader.loadRules();
        log.info("Loaded {} fraud rules", rules.size());
        for (FraudRule rule : rules) {
            if (rule.isEnabled()) {
                compileRule(rule);
            }
        }
    }

    private void compileRule(FraudRule rule) {
        try {
            ExpressionEvaluator ee = new ExpressionEvaluator(
                rule.getExpression(),                 // expression
                boolean.class,                         // return type
                new String[]{
                    "amount", "txCount24h", "avgAmount24h",
                    "geoVelocity", "deviceRiskScore", "ipReputation",
                    "timeAnomaly", "amountZscore", "merchantDiversity",
                    "nightTxRatio"
                },
                new Class[]{
                    double.class, int.class, double.class,
                    double.class, double.class, double.class,
                    double.class, double.class, int.class,
                    double.class
                }
            );
            evaluatorCache.put(rule.getRuleId(), ee);
            log.debug("Compiled rule: {} → {}", rule.getRuleId(), rule.getExpression());
        } catch (Exception e) {
            log.error("Failed to compile rule {}: {}", rule.getRuleId(), e.getMessage());
        }
    }

    /**
     * Evaluate all enabled rules against the current transaction.
     */
    public List<RuleResult> evaluate(Transaction tx, FeatureVector features, PipelineContext ctx) {
        List<RuleResult> results = new java.util.ArrayList<>();

        // Build variable array once for all rules
        Object[] vars = new Object[]{
            tx.getAmount(),                         // amount
            (int) features.getTxCount24h(),         // txCount24h
            features.getAvgAmount24h(),             // avgAmount24h
            features.getGeoVelocity(),              // geoVelocity
            features.getDeviceRiskScore(),           // deviceRiskScore
            features.getIpReputation(),              // ipReputation
            features.getTimeAnomaly(),               // timeAnomaly
            features.getAmountZscore(),              // amountZscore
            (int) features.getMerchantDiversity(),   // merchantDiversity
            features.getNightTxRatio()               // nightTxRatio
        };

        for (FraudRule rule : rules) {
            if (!rule.isEnabled()) continue;

            ExpressionEvaluator ee = evaluatorCache.get(rule.getRuleId());
            if (ee == null) continue;

            try {
                boolean triggered = (boolean) ee.evaluate(vars);
                results.add(RuleResult.create(rule.getRuleId(), rule.getName())
                    .withTriggered(triggered)
                    .withWeight(triggered ? rule.getScore() : 0)
                    .withExpression(rule.getExpression()));
            } catch (Exception e) {
                log.warn("Rule evaluation failed: {} - {}", rule.getRuleId(), e.getMessage());
                results.add(RuleResult.create(rule.getRuleId(), rule.getName())
                    .withTriggered(false).withWeight(0));
            }
        }

        return results;
    }

    /** Hot-reload rules at runtime (triggered via management endpoint). */
    public void reloadRules() {
        evaluatorCache.clear();
        this.rules = configLoader.loadRules();
        for (FraudRule rule : rules) {
            if (rule.isEnabled()) compileRule(rule);
        }
        log.info("Reloaded {} rules", rules.size());
    }
}
