package com.fraud.detection.rule;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads fraud rules from YAML configuration.
 * Supports file-based and classpath-based loading.
 */
@ApplicationScoped
public class RuleConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(RuleConfigLoader.class);
    private static final String DEFAULT_CONFIG_PATH = "rules-config.yaml";

    /** Wrapper for YAML structure */
    public static class RuleConfig {
        public List<FraudRule> rules = new ArrayList<>();
        public List<FraudRule> getRules() { return rules; }
        public void setRules(List<FraudRule> rules) { this.rules = rules; }
    }

    public List<FraudRule> loadRules() {
        return loadRules(DEFAULT_CONFIG_PATH);
    }

    public List<FraudRule> loadRules(String path) {
        try {
            Yaml yaml = new Yaml(new Constructor(RuleConfig.class));

            // Try classpath first
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is != null) {
                RuleConfig config = yaml.load(is);
                log.info("Loaded {} rules from classpath: {}", config.rules.size(), path);
                return config.rules;
            }

            // Fallback to file system
            try (FileInputStream fis = new FileInputStream(path)) {
                RuleConfig config = yaml.load(fis);
                log.info("Loaded {} rules from file: {}", config.rules.size(), path);
                return config.rules;
            }
        } catch (Exception e) {
            log.warn("Could not load rules from {}: {}. Using defaults.", path, e.getMessage());
            return getDefaultRules();
        }
    }

    /** Built-in default rules when config file is unavailable. */
    private List<FraudRule> getDefaultRules() {
        List<FraudRule> defaults = new ArrayList<>();
        defaults.add(new FraudRule(
            "RULE_001", "大额+异地",
            "amount > 10000 && geoVelocity > 500",
            0.8, 30));
        defaults.add(new FraudRule(
            "RULE_002", "24h频次异常",
            "txCount24h > 10 && deviceRiskScore > 0.6",
            0.6, 25));
        defaults.add(new FraudRule(
            "RULE_003", "深夜+新设备",
            "nightTxRatio > 0.3 && deviceRiskScore > 0.5",
            0.5, 20));
        defaults.add(new FraudRule(
            "RULE_004", "金额极端异常",
            "amountZscore > 3.0",
            0.7, 35));
        defaults.add(new FraudRule(
            "RULE_005", "IP信誉低",
            "ipReputation < 0.3 && amount > 5000",
            0.5, 20));
        return defaults;
    }
}
