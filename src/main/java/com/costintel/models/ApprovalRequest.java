package com.costintel.models;

public class ApprovalRequest {
    private String id;
    private String playbookId;
    private String agent;
    private String description;
    private String riskLevel; // HIGH, MEDIUM, LOW
    private double estimatedImpact;
    private String status; // PENDING, APPROVED, REJECTED
    private String createdAt;
    private String resolvedAt;

    public ApprovalRequest() {}

    public ApprovalRequest(String id, String playbookId, String agent, String description,
                           String riskLevel, double estimatedImpact, String status, String createdAt) {
        this.id = id;
        this.playbookId = playbookId;
        this.agent = agent;
        this.description = description;
        this.riskLevel = riskLevel;
        this.estimatedImpact = estimatedImpact;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlaybookId() { return playbookId; }
    public void setPlaybookId(String playbookId) { this.playbookId = playbookId; }

    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public double getEstimatedImpact() { return estimatedImpact; }
    public void setEstimatedImpact(double estimatedImpact) { this.estimatedImpact = estimatedImpact; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }
}
