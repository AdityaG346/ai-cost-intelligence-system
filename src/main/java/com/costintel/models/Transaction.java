package com.costintel.models;

import com.fasterxml.jackson.annotation.JsonAlias;

public class Transaction {
    private String id;
    private String vendor;
    private double amount;
    private String date;
    @JsonAlias("usagePercent")
    private double resourceUsage;
    @JsonAlias("expectedCost")
    private double expectedAmount;
    @JsonAlias("actualCost")
    private double actualAmount;
    private String category;
    private String status;
    private String deadline;

    public Transaction() {}

    public Transaction(String id, String vendor, double amount, String date,
                       double resourceUsage, double expectedAmount, double actualAmount,
                       String category, String status, String deadline) {
        this.id = id;
        this.vendor = vendor;
        this.amount = amount;
        this.date = date;
        this.resourceUsage = resourceUsage;
        this.expectedAmount = expectedAmount;
        this.actualAmount = actualAmount;
        this.category = category;
        this.status = status;
        this.deadline = deadline;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getResourceUsage() { return resourceUsage; }
    public void setResourceUsage(double resourceUsage) { this.resourceUsage = resourceUsage; }

    public double getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(double expectedAmount) { this.expectedAmount = expectedAmount; }

    public double getActualAmount() { return actualAmount; }
    public void setActualAmount(double actualAmount) { this.actualAmount = actualAmount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
}
