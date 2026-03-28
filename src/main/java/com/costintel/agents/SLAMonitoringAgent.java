package com.costintel.agents;

import com.costintel.models.ActionItem;
import com.costintel.models.Issue;
import com.costintel.models.Playbook;
import com.costintel.models.Transaction;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class SLAMonitoringAgent implements Agent {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String getName() {
        return "SLA Monitoring Agent";
    }

    @Override
    public List<Issue> detect(List<Transaction> transactions) {
        List<Issue> issues = new ArrayList<>();
        int issueCount = 0;
        LocalDate today = LocalDate.now();

        for (Transaction t : transactions) {
            if (t.getDeadline() == null || t.getDeadline().isEmpty()) continue;

            try {
                LocalDate deadline = LocalDate.parse(t.getDeadline(), FMT);
                long daysUntil = ChronoUnit.DAYS.between(today, deadline);

                if (daysUntil < 0) {
                    // SLA Breach — already past deadline
                    double penaltyCost = t.getAmount() * 0.1;
                    issues.add(new Issue(
                        "SLA-" + (++issueCount),
                        "SLA_BREACH",
                        "CRITICAL",
                        "SLA BREACH: " + t.getVendor() + " deadline was " + t.getDeadline() + " (" + Math.abs(daysUntil) + " days overdue). Penalty exposure: ₹" + String.format("%.0f", penaltyCost),
                        getName(),
                        t.getId(),
                        "Escalate SLA breach, apply penalty clause, and reroute remaining work",
                        penaltyCost
                    ));
                } else if (daysUntil <= 3) {
                    // SLA Warning — deadline approaching, trigger early intervention
                    double riskExposure = t.getAmount() * 0.05;
                    issues.add(new Issue(
                        "SLA-" + (++issueCount),
                        "SLA_AT_RISK",
                        "HIGH",
                        "SLA at risk: " + t.getVendor() + " deadline " + t.getDeadline() + " (" + daysUntil + " days remaining). Risk exposure: ₹" + String.format("%.0f", riskExposure),
                        getName(),
                        t.getId(),
                        "Shift resources and reroute work to meet deadline; escalate if blocked",
                        riskExposure
                    ));
                } else if (daysUntil <= 7 && t.getResourceUsage() > 0 && t.getResourceUsage() < 50) {
                    // SLA at risk due to low resource utilization with approaching deadline
                    double riskExposure = t.getAmount() * 0.03;
                    issues.add(new Issue(
                        "SLA-" + (++issueCount),
                        "SLA_RESOURCE_RISK",
                        "MEDIUM",
                        "Resource bottleneck risk: " + t.getVendor() + " at " + t.getResourceUsage() + "% utilization with deadline in " + daysUntil + " days",
                        getName(),
                        t.getId(),
                        "Increase resource allocation or redistribute workload to prevent SLA breach",
                        riskExposure
                    ));
                }
            } catch (Exception e) {
                // Skip transactions with invalid dates
            }
        }
        return issues;
    }

    @Override
    public List<Issue> analyze(List<Issue> issues) {
        for (Issue issue : issues) {
            if ("SLA_BREACH".equals(issue.getType())) {
                issue.setSeverity("CRITICAL");
            }
        }
        return issues;
    }

    @Override
    public List<ActionItem> act(List<Issue> issues) {
        List<ActionItem> actions = new ArrayList<>();
        int actionCount = 0;
        for (Issue issue : issues) {
            String actionType;
            String description;
            String status = "EXECUTED";

            switch (issue.getType()) {
                case "SLA_BREACH":
                    actionType = "ESCALATE_AND_REROUTE";
                    description = "Escalated SLA breach. Penalty clause activated. Initiated work rerouting to backup vendor. " + issue.getDescription();
                    break;
                case "SLA_AT_RISK":
                    actionType = "SHIFT_RESOURCES";
                    description = "Triggered resource shift: additional team allocated to prevent breach. Sent urgent notification to stakeholders. " + issue.getDescription();
                    status = "IN_PROGRESS";
                    break;
                case "SLA_RESOURCE_RISK":
                    actionType = "INCREASE_ALLOCATION";
                    description = "Submitted resource reallocation request. Workload redistribution initiated. " + issue.getDescription();
                    status = "PENDING_APPROVAL";
                    break;
                default:
                    actionType = "SEND_WARNING";
                    description = "Sent warning notification. " + issue.getDescription();
            }

            actions.add(new ActionItem(
                "ACT-SLA-" + (++actionCount),
                actionType,
                description,
                status,
                issue.getPotentialSavings(),
                issue.getId()
            ));
        }
        return actions;
    }

    @Override
    public List<Playbook> generatePlaybooks(List<Issue> issues) {
        List<Playbook> playbooks = new ArrayList<>();
        int pbCount = 0;

        for (Issue issue : issues) {
            List<String> steps;
            String costMath;
            String title;

            switch (issue.getType()) {
                case "SLA_BREACH":
                    title = "SLA Breach Recovery & Penalty Mitigation";
                    steps = Arrays.asList(
                        "1. Document breach: capture deadline miss evidence and root cause",
                        "2. Activate penalty clause per contract terms",
                        "3. Reroute remaining deliverables to backup vendor(s)",
                        "4. Negotiate penalty reduction if vendor proposes remediation plan",
                        "5. Update SLA tracking dashboard and notify all stakeholders",
                        "6. Schedule post-mortem review within 5 business days"
                    );
                    costMath = "Penalty Amount: ₹" + String.format("%.0f", issue.getPotentialSavings())
                        + " (10% of contract value) | Rerouting Cost: ~₹" + String.format("%.0f", issue.getPotentialSavings() * 0.5)
                        + " | Net Recovery: ₹" + String.format("%.0f", issue.getPotentialSavings() * 0.5);
                    break;
                case "SLA_AT_RISK":
                    title = "Prevent SLA Breach — Emergency Resource Shift";
                    steps = Arrays.asList(
                        "1. Assess current progress vs. remaining deliverables",
                        "2. Allocate additional resources from internal pool",
                        "3. Notify vendor of escalation and request daily status updates",
                        "4. Set up hourly progress tracking until deadline",
                        "5. Prepare contingency: identify backup vendor for partial handoff"
                    );
                    costMath = "Risk Exposure: ₹" + String.format("%.0f", issue.getPotentialSavings())
                        + " | Prevention Cost: ~₹" + String.format("%.0f", issue.getPotentialSavings() * 0.3)
                        + " | Savings by Prevention: ₹" + String.format("%.0f", issue.getPotentialSavings() * 0.7);
                    break;
                default:
                    title = "SLA Risk Mitigation";
                    steps = Arrays.asList(
                        "1. Assess resource utilization and workload distribution",
                        "2. Reallocate resources to at-risk deliverables",
                        "3. Monitor daily until risk is mitigated"
                    );
                    costMath = "Risk Exposure: ₹" + String.format("%.0f", issue.getPotentialSavings());
            }

            playbooks.add(new Playbook(
                "PB-SLA-" + (++pbCount),
                title,
                getName(),
                issue.getId(),
                steps,
                costMath,
                issue.getPotentialSavings(),
                "GENERATED",
                issue.getSeverity()
            ));
        }
        return playbooks;
    }
}
