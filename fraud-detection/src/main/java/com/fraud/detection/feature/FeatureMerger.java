package com.fraud.detection.feature;

import com.fraud.detection.pipeline.PipelineContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.codehaus.janino.ExpressionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Merges model score, rules score, and scanners score into a final fraud score.
 *
 * The merge formula is defined as a Janino expression, allowing runtime
 * weight adjustments without code changes.
 *
 * Default formula:
 *   finalScore = modelScore * 0.5 + rulesScore * 0.3 + scannersScore * 0.2
 */
@ApplicationScoped
public class FeatureMerger {

    private static final Logger log = LoggerFactory.getLogger(FeatureMerger.class);

    private volatile ExpressionEvaluator evaluator;

    /** Default merge expression — can be hot-reloaded. */
    private static final String DEFAULT_EXPR =
        "finalScore = modelScore * 0.5 + rulesScore * 0.3 + scannerScore * 0.2";

    public FeatureMerger() {
        compile(DEFAULT_EXPR);
    }

    public double merge(double modelScore, double rulesScore,
                        double scannerScore, PipelineContext ctx) {
        try {
            ExpressionEvaluator ee = this.evaluator;
            if (ee == null) {
                return fallbackMerge(modelScore, rulesScore, scannerScore);
            }
            double finalScore = (double) ee.evaluate(new Object[]{
                modelScore, rulesScore, scannerScore
            });
            return clamp(finalScore, 0, 100);
        } catch (Exception e) {
            log.warn("Janino merge failed, using fallback: {}", e.getMessage());
            return fallbackMerge(modelScore, rulesScore, scannerScore);
        }
    }

    /** Hot-reload the merge expression at runtime. */
    public void reloadExpression(String janinoExpr) {
        compile(janinoExpr);
        log.info("FeatureMerger expression reloaded: {}", janinoExpr);
    }

    private void compile(String expr) {
        try {
            ExpressionEvaluator ee = new ExpressionEvaluator(
                expr,
                double.class,
                new String[]{"modelScore", "rulesScore", "scannerScore"},
                new Class[]{double.class, double.class, double.class}
            );
            this.evaluator = ee;
        } catch (Exception e) {
            log.error("Failed to compile merger expression: {}", e.getMessage());
            this.evaluator = null;
        }
    }

    private double fallbackMerge(double model, double rules, double scanner) {
        return clamp(model * 0.5 + rules * 0.3 + scanner * 0.2, 0, 100);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
