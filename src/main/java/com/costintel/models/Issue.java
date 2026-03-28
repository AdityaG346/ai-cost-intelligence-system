package com.costintel.models;

public class Issue {
    private String id;
    private String type;
    private String severity;
    private String description;
    private String agent;
    private String affectedTransactionId;
    private String suggestedAction;
    private double potentialSavings;

    public Issue() {}

    public Issue(String id, String type, String severity, String description,
                 String agent, String affectedTransactionId, String suggestedAction,
                 double potentialSavings) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.description = description;
        this.agent = agent;
        this.affectedTransactionId = affectedTransactionId;
        this.suggestedAction = suggestedAction;
        this.potentialSavings = potentialSavings;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }

    public String getAffectedTransactionId() { return affectedTransactionId; }
    public void setAffectedTransactionId(String affectedTransactionId) { this.affectedTransactionId = affectedTransactionId; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public double getPotentialSavings() { return potentialSavings; }
    public void setPotentialSavings(double potentialSavings) { this.potentialSavings = potentialSavings; }
}
