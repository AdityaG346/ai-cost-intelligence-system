package com.costintel.models;

public class ActionItem {
    private String id;
    private String type;
    private String description;
    private String status;
    private double savings;
    private String relatedIssueId;

    public ActionItem() {}

    public ActionItem(String id, String type, String description, String status,
                      double savings, String relatedIssueId) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.status = status;
        this.savings = savings;
        this.relatedIssueId = relatedIssueId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getSavings() { return savings; }
    public void setSavings(double savings) { this.savings = savings; }

    public String getRelatedIssueId() { return relatedIssueId; }
    public void setRelatedIssueId(String relatedIssueId) { this.relatedIssueId = relatedIssueId; }
}
