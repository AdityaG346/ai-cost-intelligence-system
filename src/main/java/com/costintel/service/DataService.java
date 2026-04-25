package com.costintel.service;

import com.costintel.models.*;
import com.costintel.utils.SampleDataLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DataService {

    private final SampleDataLoader sampleDataLoader;
    private List<Transaction> transactions = new ArrayList<>();
    private List<Issue> issues = new ArrayList<>();
    private List<ActionItem> actions = new ArrayList<>();
    private List<Playbook> playbooks = new ArrayList<>();
    private Impact impact = new Impact();
    private final AtomicInteger streamedTransactionCount = new AtomicInteger(0);

    public DataService(SampleDataLoader sampleDataLoader) {
        this.sampleDataLoader = sampleDataLoader;
    }

    @PostConstruct
    public void init() {
        transactions = sampleDataLoader.loadSampleData();
    }

    public synchronized List<Transaction> getTransactions() { return new ArrayList<>(transactions); }
    public synchronized void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    public synchronized Transaction addTransaction(Transaction transaction) {
        if (transaction.getId() == null || transaction.getId().isBlank()) {
            transaction.setId(String.format("TXN-STREAM-%03d", streamedTransactionCount.incrementAndGet()));
        }
        if (transaction.getDate() == null || transaction.getDate().isBlank()) {
            transaction.setDate(LocalDate.now().toString());
        }
        if (transaction.getStatus() == null || transaction.getStatus().isBlank()) {
            transaction.setStatus("ACTIVE");
        }
        if (transaction.getExpectedAmount() == 0) {
            transaction.setExpectedAmount(transaction.getAmount());
        }
        if (transaction.getActualAmount() == 0) {
            transaction.setActualAmount(transaction.getAmount());
        }
        if (transaction.getCategory() == null || transaction.getCategory().isBlank()) {
            transaction.setCategory(inferCategory(transaction.getVendor()));
        }
        if (transaction.getDeadline() == null) {
            transaction.setDeadline("");
        }
        transactions.add(transaction);
        return transaction;
    }

    public List<Issue> getIssues() { return issues; }
    public void setIssues(List<Issue> issues) { this.issues = issues; }

    public List<ActionItem> getActions() { return actions; }
    public void setActions(List<ActionItem> actions) { this.actions = actions; }

    public List<Playbook> getPlaybooks() { return playbooks; }
    public void setPlaybooks(List<Playbook> playbooks) { this.playbooks = playbooks; }

    public Impact getImpact() { return impact; }
    public void setImpact(Impact impact) { this.impact = impact; }

    public void clearResults() {
        issues.clear();
        actions.clear();
        playbooks.clear();
        impact = new Impact();
    }

    private String inferCategory(String vendor) {
        if (vendor == null) return "General";
        String normalizedVendor = vendor.toLowerCase();
        if (normalizedVendor.contains("aws") || normalizedVendor.contains("azure") || normalizedVendor.contains("cloud")) {
            return "Infrastructure";
        }
        if (normalizedVendor.contains("google") || normalizedVendor.contains("salesforce")) {
            return "Software";
        }
        if (normalizedVendor.contains("secure")) {
            return "Security";
        }
        return "General";
    }
}
