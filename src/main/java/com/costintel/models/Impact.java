package com.costintel.models;

import java.util.HashMap;
import java.util.Map;

public class Impact {
    private double totalCost;
    private double totalWaste;
    private double totalSavings;
    private double monthlySavings;
    private double yearlySavings;
    private Map<String, Double> wasteBreakdown;

    public Impact() {
        this.wasteBreakdown = new HashMap<>();
    }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public double getTotalWaste() { return totalWaste; }
    public void setTotalWaste(double totalWaste) { this.totalWaste = totalWaste; }

    public double getTotalSavings() { return totalSavings; }
    public void setTotalSavings(double totalSavings) { this.totalSavings = totalSavings; }

    public double getMonthlySavings() { return monthlySavings; }
    public void setMonthlySavings(double monthlySavings) { this.monthlySavings = monthlySavings; }

    public double getYearlySavings() { return yearlySavings; }
    public void setYearlySavings(double yearlySavings) { this.yearlySavings = yearlySavings; }

    public Map<String, Double> getWasteBreakdown() { return wasteBreakdown; }
    public void setWasteBreakdown(Map<String, Double> wasteBreakdown) { this.wasteBreakdown = wasteBreakdown; }
}
