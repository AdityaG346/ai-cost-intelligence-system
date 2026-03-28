package com.costintel.service;

import com.costintel.models.*;
import com.costintel.utils.SampleDataLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataService {

    private final SampleDataLoader sampleDataLoader;
    private List<Transaction> transactions = new ArrayList<>();
    private List<Issue> issues = new ArrayList<>();
    private List<ActionItem> actions = new ArrayList<>();
    private List<Playbook> playbooks = new ArrayList<>();
    private Impact impact = new Impact();

    public DataService(SampleDataLoader sampleDataLoader) {
        this.sampleDataLoader = sampleDataLoader;
    }

    @PostConstruct
    public void init() {
        transactions = sampleDataLoader.loadSampleData();
    }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

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
}
