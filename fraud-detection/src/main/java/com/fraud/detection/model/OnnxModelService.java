package com.fraud.detection.model;

import com.fraud.detection.model.FeatureVector;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ONNX model inference service.
 *
 * Loads a pre-trained LightGBM-style model exported as ONNX.
 * Model file: models/fraud_model.onnx (classpath or external path)
 */
@ApplicationScoped
public class OnnxModelService {

    private static final Logger log = LoggerFactory.getLogger(OnnxModelService.class);
    private static final String MODEL_PATH = "models/fraud_model.onnx";
    private static final String INPUT_NAME = "features";
    private static final String OUTPUT_NAME = "fraud_score";

    private onnxruntime.InferenceSession session;
    private boolean available = false;

    @PostConstruct
    void init() {
        try {
            byte[] modelBytes = loadModel();
            if (modelBytes == null) {
                log.warn("ONNX model not found at {}. Running in fallback mode.", MODEL_PATH);
                return;
            }
            var options = new onnxruntime.SessionOptions();
            options.setOptimizationLevel(onnxruntime.GraphOptimizationLevel.ORT_ENABLE_ALL);
            // Enable CUDA if available
            // options.setExecutionProvider("CUDA");
            session = new onnxruntime.InferenceSession(modelBytes, options);
            available = true;
            log.info("ONNX model loaded: {} features → 1 score", INPUT_NAME);
        } catch (Exception e) {
            log.warn("Failed to load ONNX model: {}. Using fallback scoring.", e.getMessage());
        }
    }

    /**
     * Score a single transaction feature vector.
     * Returns: fraud score [0, 100], or fallback score if model unavailable.
     */
    public double score(FeatureVector features) {
        if (!available) {
            return fallbackScore(features);
        }

        try {
            float[] inputData = features.toFloatArray();
            var inputTensor = onnxruntime.OnnxTensor.createTensor(
                java.nio.FloatBuffer.wrap(inputData),
                new long[]{1, FeatureVector.FEATURE_COUNT}
            );
            var output = session.run(java.util.Map.of(INPUT_NAME, inputTensor));
            var outputTensor = (onnxruntime.OnnxTensor) output.get(OUTPUT_NAME);
            float[] result = outputTensor.getFloatBuffer().array();
            return clamp(result[0], 0, 100);
        } catch (Exception e) {
            log.warn("ONNX inference failed: {}", e.getMessage());
            return fallbackScore(features);
        }
    }

    /** Fallback scoring when model is not available (POC mode). */
    private double fallbackScore(FeatureVector fv) {
        // Simple heuristic: weighted sum of key features
        double score =
            fv.getAmountNormalized() * 5 +
            (fv.getTxCount24h() > 10 ? 15 : 0) +
            fv.getDeviceRiskScore() * 30 +
            (1 - fv.getIpReputation()) * 30 +
            fv.getTimeAnomaly() * 20;
        return clamp(score, 0, 100);
    }

    private byte[] loadModel() {
        // 1. Try absolute path (Docker/K8s volume mount)
        try {
            Path extPath = Paths.get("/models/fraud_model.onnx");
            if (Files.exists(extPath)) return Files.readAllBytes(extPath);
        } catch (Exception ignored) {}

        // 2. Try classpath
        try (var is = getClass().getClassLoader().getResourceAsStream(MODEL_PATH)) {
            if (is != null) return is.readAllBytes();
        } catch (Exception ignored) {}

        // 3. Try current working directory
        try {
            Path localPath = Paths.get(MODEL_PATH);
            if (Files.exists(localPath)) return Files.readAllBytes(localPath);
        } catch (Exception ignored) {}

        return null;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public boolean isAvailable() { return available; }
}
