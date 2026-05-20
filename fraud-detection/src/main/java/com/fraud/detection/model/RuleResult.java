package com.fraud.detection.model;

/**
 * Result from a single rule evaluation.
 */
public class RuleResult {
    private String ruleId;
    private String ruleName;
    private boolean triggered;
    private double ruleWeight;
    private String expression;

    public RuleResult() {}

    public static RuleResult create(String ruleId, String ruleName) {
        RuleResult r = new RuleResult();
        r.ruleId = ruleId;
        r.ruleName = ruleName;
        return r;
    }

    public RuleResult withTriggered(boolean t)  { this.triggered = t; return this; }
    public RuleResult withWeight(double w)      { this.ruleWeight = w; return this; }
    public RuleResult withExpression(String e)  { this.expression = e; return this; }

    public String getRuleId()      { return ruleId; }
    public String getRuleName()    { return ruleName; }
    public boolean isTriggered()   { return triggered; }
    public double getRuleWeight()  { return ruleWeight; }
    public String getExpression()  { return expression; }
}
