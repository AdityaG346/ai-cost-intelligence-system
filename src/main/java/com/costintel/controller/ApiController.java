package com.costintel.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.costintel.models.ActionItem;
import com.costintel.models.ApprovalRequest;
import com.costintel.models.AuditEntry;
import com.costintel.models.Impact;
import com.costintel.models.Playbook;
import com.costintel.models.Transaction;
import com.costintel.service.ApprovalService;
import com.costintel.service.DataService;
import com.costintel.service.WorkflowEngine;
import com.costintel.utils.AuditLogger;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final DataService dataService;
    private final WorkflowEngine workflowEngine;
    private final AuditLogger auditLogger;
    private final ApprovalService approvalService;

    public ApiController(DataService dataService, WorkflowEngine workflowEngine,
                         AuditLogger auditLogger, ApprovalService approvalService) {
        this.dataService = dataService;
        this.workflowEngine = workflowEngine;
        this.auditLogger = auditLogger;
        this.approvalService = approvalService;
    }

    @GetMapping("/data")
    public List<Transaction> getTransactions() {
        return dataService.getTransactions();
    }

    @PostMapping("/ingest/transaction")
    public Transaction ingestTransaction(@RequestBody Transaction transaction) {
        return dataService.addTransaction(transaction);
    }

    @PostMapping("/analyze")
    public Map<String, Object> runAnalysis() {
        return workflowEngine.runFullPipeline();
    }

    @GetMapping("/results")
    public Map<String, Object> getResults() {
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("issues", dataService.getIssues());
        results.put("actions", dataService.getActions());
        return results;
    }

    @GetMapping("/impact")
    public Impact getImpact() {
        return dataService.getImpact();
    }

    @GetMapping("/audit")
    public List<AuditEntry> getAuditLog() {
        return auditLogger.getAuditLog();
    }

    @GetMapping("/agents")
    public Map<String, Map<String, Object>> getAgentActivity() {
        return workflowEngine.getAgentActivity();
    }

    @GetMapping("/actions")
    public List<ActionItem> getActions() {
        return dataService.getActions();
    }

    @GetMapping("/playbooks")
    public List<Playbook> getPlaybooks() {
        return dataService.getPlaybooks();
    }

    @GetMapping("/approvals")
    public List<ApprovalRequest> getApprovals() {
        return approvalService.getAllRequests();
    }

    @PostMapping("/approvals/{id}/approve")
    public ResponseEntity<?> approveRequest(@PathVariable String id) {
        Optional<ApprovalRequest> result = approvalService.approveRequest(id);
        if (result.isPresent()) {
            ApprovalRequest approval = result.get();
            auditLogger.log("Approval System", 
                "Approved request " + approval.getId() + 
                " | Agent: " + approval.getAgent() + 
                " | Action: " + approval.getDescription() + 
                " | Savings: ₹" + String.format("%.0f", approval.getEstimatedImpact()) +
                " | Risk Level: " + approval.getRiskLevel());
            return ResponseEntity.ok(approval);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/approvals/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable String id) {
        Optional<ApprovalRequest> result = approvalService.rejectRequest(id);
        if (result.isPresent()) {
            ApprovalRequest approval = result.get();
            auditLogger.log("Approval System", 
                "Rejected request " + approval.getId() + 
                " | Agent: " + approval.getAgent() + 
                " | Action: " + approval.getDescription() + 
                " | Savings: ₹" + String.format("%.0f", approval.getEstimatedImpact()) +
                " | Risk Level: " + approval.getRiskLevel());
            return ResponseEntity.ok(approval);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/dashboard/summary")
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        Impact impact = dataService.getImpact();
        List<ActionItem> actions = dataService.getActions();
        List<Playbook> playbooks = dataService.getPlaybooks();

        summary.put("impact", impact);
        summary.put("totalIssues", dataService.getIssues().size());
        summary.put("totalActions", actions.size());
        summary.put("totalPlaybooks", playbooks.size());
        summary.put("pendingApprovals", approvalService.getPendingRequests().size());

        // ROI metrics
        long executedActions = actions.stream().filter(a -> "EXECUTED".equals(a.getStatus())).count();
        long pendingActions = actions.stream().filter(a -> "PENDING_APPROVAL".equals(a.getStatus())).count();
        double executedSavings = actions.stream()
            .filter(a -> "EXECUTED".equals(a.getStatus()))
            .mapToDouble(ActionItem::getSavings).sum();
        double pendingSavings = actions.stream()
            .filter(a -> "PENDING_APPROVAL".equals(a.getStatus()))
            .mapToDouble(ActionItem::getSavings).sum();
        double efficiency = impact.getTotalCost() > 0
            ? (impact.getTotalSavings() / impact.getTotalCost()) * 100 : 0;

        summary.put("executedActions", executedActions);
        summary.put("pendingActions", pendingActions);
        summary.put("executedSavings", executedSavings);
        summary.put("pendingSavings", pendingSavings);
        summary.put("efficiencyPercent", Math.round(efficiency * 100.0) / 100.0);

        return summary;
    }
}
