package com.costintel.controller;

import com.costintel.models.*;
import com.costintel.service.ApprovalService;
import com.costintel.service.DataService;
import com.costintel.service.WorkflowEngine;
import com.costintel.utils.AuditLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            auditLogger.log("Approval System", "Approved request " + id + ": " + result.get().getDescription());
            return ResponseEntity.ok(result.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/approvals/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable String id) {
        Optional<ApprovalRequest> result = approvalService.rejectRequest(id);
        if (result.isPresent()) {
            auditLogger.log("Approval System", "Rejected request " + id + ": " + result.get().getDescription());
            return ResponseEntity.ok(result.get());
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
