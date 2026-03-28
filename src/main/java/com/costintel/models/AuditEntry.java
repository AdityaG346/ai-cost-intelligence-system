package com.costintel.models;

public class AuditEntry {
    private String timestamp;
    private int step;
    private String agent;
    private String message;

    public AuditEntry() {}

    public AuditEntry(String timestamp, int step, String agent, String message) {
        this.timestamp = timestamp;
        this.step = step;
        this.agent = agent;
        this.message = message;
    }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }

    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
