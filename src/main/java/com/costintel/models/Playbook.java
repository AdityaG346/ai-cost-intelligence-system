package com.costintel.models;

import java.util.ArrayList;
import java.util.List;

public class Playbook {
    private String id;
    private String title;
    private String agent;
    private String triggerIssueId;
    private List<String> steps;
    private String costMath;
    private double estimatedSavings;
    private String status; // GENERATED, APPROVED, EXECUTED, REJECTED
    private String priority; // CRITICAL, HIGH, MEDIUM, LOW

    public Playbook() {
        this.steps = new ArrayList<>();
    }

    public Playbook(String id, String title, String agent, String triggerIssueId,
                    List<String> steps, String costMath, double estimatedSavings,
                    String status, String priority) {
        this.id = id;
        this.title = title;
        this.agent = agent;
        this.triggerIssueId = triggerIssueId;
        this.steps = steps != null ? steps : new ArrayList<>();
        this.costMath = costMath;
        this.estimatedSavings = estimatedSavings;
        this.status = status;
        this.priority = priority;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }

    public String getTriggerIssueId() { return triggerIssueId; }
    public void setTriggerIssueId(String triggerIssueId) { this.triggerIssueId = triggerIssueId; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public String getCostMath() { return costMath; }
    public void setCostMath(String costMath) { this.costMath = costMath; }

    public double getEstimatedSavings() { return estimatedSavings; }
    public void setEstimatedSavings(double estimatedSavings) { this.estimatedSavings = estimatedSavings; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
