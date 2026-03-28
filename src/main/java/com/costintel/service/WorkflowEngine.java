package com.costintel.service;

import com.costintel.agents.Agent;
import com.costintel.models.*;
import com.costintel.utils.AuditLogger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class WorkflowEngine {

    private final DataService dataService;
    private final AuditLogger auditLogger;
    private final ApprovalService approvalService;
    private final List<Agent> agents;
    private final Map<String, Map<String, Object>> agentActivity = new LinkedHashMap<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WorkflowEngine(DataService dataService, AuditLogger auditLogger,
                          ApprovalService approvalService, List<Agent> agents) {
        this.dataService = dataService;
        this.auditLogger = auditLogger;
        this.approvalService = approvalService;
        this.agents = agents;
    }

    public Map<String, Object> runFullPipeline() {
        auditLogger.clear();
        dataService.clearResults();
        approvalService.clear();
        agentActivity.clear();

        List<Transaction> transactions = dataService.getTransactions();
        List<Issue> allIssues = new ArrayList<>();
        List<ActionItem> allActions = new ArrayList<>();
        List<Playbook> allPlaybooks = new ArrayList<>();

        // Step 1: Load data
        auditLogger.log("System", "Data loaded: " + transactions.size() + " transactions");

        // Step 2-4: Run each agent through detect → analyze → act → generate playbooks
        int approvalCount = 0;
        for (Agent agent : agents) {
            auditLogger.log(agent.getName(), "Starting detection phase");

            // DETECT
            List<Issue> detected = agent.detect(transactions);
            auditLogger.log(agent.getName(), "Detected " + detected.size() + " issues");

            // ANALYZE
            List<Issue> analyzed = agent.analyze(detected);
            auditLogger.log(agent.getName(), "Analysis complete. " + analyzed.size() + " issues confirmed");

            // ACT
            List<ActionItem> actions = agent.act(analyzed);
            auditLogger.log(agent.getName(), "Executed " + actions.size() + " corrective actions");

            // GENERATE PLAYBOOKS
            List<Playbook> playbooks = agent.generatePlaybooks(analyzed);
            if (!playbooks.isEmpty()) {
                auditLogger.log(agent.getName(), "Generated " + playbooks.size() + " actionable playbooks");
            }

            // CREATE APPROVAL REQUESTS for high-risk actions
            int agentApprovals = 0;
            for (ActionItem action : actions) {
                if ("PENDING_APPROVAL".equals(action.getStatus())) {
                    String riskLevel = action.getSavings() > 5000 ? "HIGH" : "MEDIUM";
                    ApprovalRequest approval = new ApprovalRequest(
                        "APR-" + (++approvalCount),
                        findPlaybookForIssue(playbooks, action.getRelatedIssueId()),
                        agent.getName(),
                        action.getDescription(),
                        riskLevel,
                        action.getSavings(),
                        "PENDING",
                        LocalDateTime.now().format(FMT)
                    );
                    approvalService.addRequest(approval);
                    agentApprovals++;
                }
            }
            if (agentApprovals > 0) {
                auditLogger.log(agent.getName(), agentApprovals + " actions queued for approval");
            }

            // Track agent activity
            Map<String, Object> activity = new LinkedHashMap<>();
            activity.put("issuesDetected", detected.size());
            activity.put("actionsExecuted", actions.size());
            double agentSavings = actions.stream().mapToDouble(ActionItem::getSavings).sum();
            activity.put("totalSavings", agentSavings);
            activity.put("playbooksGenerated", playbooks.size());
            activity.put("pendingApprovals", agentApprovals);
            activity.put("issues", analyzed);
            activity.put("actions", actions);
            activity.put("playbooks", playbooks);
            agentActivity.put(agent.getName(), activity);

            allIssues.addAll(analyzed);
            allActions.addAll(actions);
            allPlaybooks.addAll(playbooks);
        }

        // Step 5: Calculate impact
        auditLogger.log("System", "Calculating financial impact");
        Impact impact = calculateImpact(transactions, allIssues, allActions);

        // Store results
        dataService.setIssues(allIssues);
        dataService.setActions(allActions);
        dataService.setPlaybooks(allPlaybooks);
        dataService.setImpact(impact);

        int totalApprovals = approvalService.getAllRequests().size();
        auditLogger.log("System", "Pipeline complete. Total savings: ₹" + String.format("%.0f", impact.getTotalSavings())
            + " | Playbooks: " + allPlaybooks.size()
            + " | Pending approvals: " + totalApprovals);

        // Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalIssues", allIssues.size());
        result.put("totalActions", allActions.size());
        result.put("totalPlaybooks", allPlaybooks.size());
        result.put("pendingApprovals", totalApprovals);
        result.put("impact", impact);
        result.put("agentActivity", agentActivity);
        result.put("actions", allActions);
        result.put("playbooks", allPlaybooks);
        result.put("approvals", approvalService.getAllRequests());
        result.put("auditLog", auditLogger.getAuditLog());
        return result;
    }

    private String findPlaybookForIssue(List<Playbook> playbooks, String issueId) {
        if (issueId == null) return null;
        for (Playbook pb : playbooks) {
            if (issueId.equals(pb.getTriggerIssueId())) {
                return pb.getId();
            }
        }
        return null;
    }

    private Impact calculateImpact(List<Transaction> transactions, List<Issue> issues, List<ActionItem> actions) {
        Impact impact = new Impact();

        double totalCost = transactions.stream().mapToDouble(Transaction::getAmount).sum();
        impact.setTotalCost(totalCost);

        double totalWaste = issues.stream().mapToDouble(Issue::getPotentialSavings).sum();
        impact.setTotalWaste(totalWaste);

        double totalSavings = actions.stream().mapToDouble(ActionItem::getSavings).sum();
        impact.setTotalSavings(totalSavings);
        impact.setMonthlySavings(totalSavings);
        impact.setYearlySavings(totalSavings * 12);

        // Waste breakdown by issue type
        Map<String, Double> breakdown = new LinkedHashMap<>();
        for (Issue issue : issues) {
            breakdown.merge(issue.getType(), issue.getPotentialSavings(), Double::sum);
        }
        impact.setWasteBreakdown(breakdown);

        return impact;
    }

    public Map<String, Map<String, Object>> getAgentActivity() {
        return agentActivity;
    }
}
