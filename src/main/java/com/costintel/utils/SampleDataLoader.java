package com.costintel.utils;

import com.costintel.models.Transaction;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class SampleDataLoader {

    public List<Transaction> loadSampleData() {
        List<Transaction> data = new ArrayList<>();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String pastDeadline = LocalDate.now().minusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nearDeadline = LocalDate.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String weekDeadline = LocalDate.now().plusDays(6).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String futureDeadline = LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // ── Normal transactions ──
        data.add(new Transaction("TXN-001", "AWS Cloud Services", 15000, "2026-03-01", 85, 15000, 15000, "Infrastructure", "COMPLETED", futureDeadline));
        data.add(new Transaction("TXN-002", "Google Workspace", 5000, "2026-03-01", 90, 5000, 5000, "Software", "COMPLETED", futureDeadline));
        data.add(new Transaction("TXN-003", "Salesforce CRM", 12000, "2026-03-05", 75, 12000, 12000, "Software", "COMPLETED", futureDeadline));

        // ── Duplicate payments (same vendor + amount + date) ──
        data.add(new Transaction("TXN-004", "Office Supplies Co", 5000, "2026-03-10", 0, 5000, 5000, "Office", "COMPLETED", ""));
        data.add(new Transaction("TXN-005", "Office Supplies Co", 5000, "2026-03-10", 0, 5000, 5000, "Office", "COMPLETED", ""));

        data.add(new Transaction("TXN-006", "Tech Solutions Ltd", 8500, "2026-03-12", 0, 8500, 8500, "Consulting", "COMPLETED", ""));
        data.add(new Transaction("TXN-007", "Tech Solutions Ltd", 8500, "2026-03-12", 0, 8500, 8500, "Consulting", "COMPLETED", ""));

        // ── SLA breaches (deadline passed) ──
        data.add(new Transaction("TXN-008", "DataPro Analytics", 20000, "2026-03-01", 60, 20000, 20000, "Analytics", "PENDING", pastDeadline));
        data.add(new Transaction("TXN-009", "SecureNet Solutions", 18000, "2026-02-20", 70, 18000, 18000, "Security", "PENDING", pastDeadline));

        // ── SLA warnings (deadline approaching within 3 days) ──
        data.add(new Transaction("TXN-010", "CloudMigrate Inc", 25000, "2026-03-15", 50, 25000, 25000, "Infrastructure", "IN_PROGRESS", nearDeadline));

        // ── SLA resource risk (deadline within 7 days + low utilization) ──
        data.add(new Transaction("TXN-011", "Azure Dev Server", 12000, "2026-03-01", 15, 12000, 12000, "Infrastructure", "ACTIVE", weekDeadline));
        data.add(new Transaction("TXN-012", "Staging Environment", 8000, "2026-03-01", 22, 8000, 8000, "Infrastructure", "ACTIVE", futureDeadline));
        data.add(new Transaction("TXN-013", "Test Lab Cluster", 10000, "2026-03-05", 10, 10000, 10000, "Infrastructure", "ACTIVE", futureDeadline));
        data.add(new Transaction("TXN-014", "Backup Storage Unit", 6000, "2026-03-08", 35, 6000, 6000, "Storage", "ACTIVE", futureDeadline));

        // ── Financial mismatches (expected != actual) with varying root causes ──
        data.add(new Transaction("TXN-015", "Marketing Agency", 30000, "2026-03-10", 0, 30000, 35000, "Marketing", "COMPLETED", ""));
        data.add(new Transaction("TXN-016", "Logistics Partner", 22000, "2026-03-12", 0, 22000, 24500, "Logistics", "COMPLETED", ""));
        data.add(new Transaction("TXN-017", "Equipment Vendor", 45000, "2026-03-15", 0, 45000, 41000, "Equipment", "COMPLETED", ""));
        data.add(new Transaction("TXN-018", "Marketing Agency", 28000, "2026-03-18", 0, 28000, 30500, "Marketing", "COMPLETED", ""));

        // ── Spending anomaly (spike for vendor) ──
        data.add(new Transaction("TXN-019", "AWS Cloud Services", 3000, "2026-03-10", 80, 3000, 3000, "Infrastructure", "COMPLETED", futureDeadline));
        data.add(new Transaction("TXN-020", "AWS Cloud Services", 55000, "2026-03-20", 95, 55000, 55000, "Infrastructure", "COMPLETED", futureDeadline));

        // ── Recurring expenses (3+ from same vendor) ──
        data.add(new Transaction("TXN-021", "Google Workspace", 5000, "2026-02-01", 88, 5000, 5000, "Software", "COMPLETED", ""));
        data.add(new Transaction("TXN-022", "Google Workspace", 5000, "2026-01-01", 92, 5000, 5000, "Software", "COMPLETED", ""));

        // ── Additional data for rate optimization ──
        data.add(new Transaction("TXN-023", "Budget Consulting", 3500, "2026-03-08", 0, 3500, 3500, "Consulting", "COMPLETED", ""));
        data.add(new Transaction("TXN-024", "Premium Analytics Ltd", 35000, "2026-03-14", 65, 35000, 35000, "Analytics", "COMPLETED", futureDeadline));

        // ── Additional underutilized resource for consolidation ──
        data.add(new Transaction("TXN-025", "Dev Sandbox Env", 7000, "2026-03-03", 18, 7000, 7000, "Infrastructure", "ACTIVE", futureDeadline));

        return data;
    }
}
