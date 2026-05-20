package com.fraud.detection.rule;

/**
 * A single fraud detection rule loaded from configuration.
 */
public class FraudRule {
    private String ruleId;
    private String name;
    private String expression;  // Janino expression
    private double weight;
    private double score;
    private boolean enabled;

    public FraudRule() {}

    public FraudRule(String ruleId, String name, String expression, double weight, double score) {
        this.ruleId = ruleId;
        this.name = name;
        this.expression = expression;
        this.weight = weight;
        this.score = score;
        this.enabled = true;
    }

    public String getRuleId()     { return ruleId; }
    public String getName()       { return name; }
    public String getExpression() { return expression; }
    public double getWeight()     { return weight; }
    public double getScore()      { return score; }
    public boolean isEnabled()    { return enabled; }

    public void setRuleId(String ruleId)       { this.ruleId = ruleId; }
    public void setName(String name)           { this.name = name; }
    public void setExpression(String expr)     { this.expression = expr; }
    public void setWeight(double weight)       { this.weight = weight; }
    public void setScore(double score)         { this.score = score; }
    public void setEnabled(boolean enabled)    { this.enabled = enabled; }
}
