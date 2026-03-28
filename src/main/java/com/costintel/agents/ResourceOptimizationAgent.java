package com.costintel.agents;

import com.costintel.models.ActionItem;
import com.costintel.models.Issue;
import com.costintel.models.Playbook;
import com.costintel.models.Transaction;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ResourceOptimizationAgent implements Agent {

    private static final double UNDERUTILIZATION_THRESHOLD = 40.0;
    private static final double SEVERE_UNDERUTILIZATION = 20.0;

    @Override
    public String getName() {
        return "Resource Optimization Agent";
    }

    @Override
    public List<Issue> detect(List<Transaction> transactions) {
        List<Issue> issues = new ArrayList<>();
        int issueCount = 0;

        List<Transaction> underutilized = new ArrayList<>();

        for (Transaction t : transactions) {
            if (t.getResourceUsage() > 0 && t.getResourceUsage() < UNDERUTILIZATION_THRESHOLD) {
                double wastedPortion = (1 - t.getResourceUsage() / 100.0) * t.getAmount();
                String severity = t.getResourceUsage() < SEVERE_UNDERUTILIZATION ? "HIGH" : "MEDIUM";
                issues.add(new Issue(
                    "RO-" + (++issueCount),
                    "UNDERUTILIZED_RESOURCE",
                    severity,
                    "Underutilized resource: " + t.getVendor() + " at " + t.getResourceUsage() + "% usage (cost: ₹" + t.getAmount() + ", wasted: ₹" + String.format("%.0f", wastedPortion) + ")",
                    getName(),
                    t.getId(),
                    "Downsize or terminate underutilized resource from " + t.getVendor(),
                    wastedPortion
                ));
                underutilized.add(t);
            }
        }

        // Consolidation opportunity — multiple underutilized resources in same category
        Map<String, List<Transaction>> categoryUnderutilized = new HashMap<>();
        for (Transaction t : underutilized) {
            if (t.getCategory() != null) {
                categoryUnderutilized.computeIfAbsent(t.getCategory(), k -> new ArrayList<>()).add(t);
            }
        }
        for (Map.Entry<String, List<Transaction>> entry : categoryUnderutilized.entrySet()) {
            if (entry.getValue().size() >= 2) {
                double totalCost = entry.getValue().stream().mapToDouble(Transaction::getAmount).sum();
                double avgUsage = entry.getValue().stream().mapToDouble(Transaction::getResourceUsage).average().orElse(0);
                double consolidatedSaving = totalCost * (1 - avgUsage / 100.0) * 0.7;
                String vendors = entry.getValue().stream()
                    .map(Transaction::getVendor)
                    .reduce((a, b) -> a + ", " + b).orElse("multiple");

                issues.add(new Issue(
                    "RO-" + (++issueCount),
                    "CONSOLIDATION_OPPORTUNITY",
                    "HIGH",
                    "Consolidation opportunity: " + entry.getValue().size() + " underutilized " + entry.getKey() + " resources (" + vendors + ") can be merged. Total cost: ₹" + String.format("%.0f", totalCost),
                    getName(),
                    null,
                    "Consolidate " + entry.getValue().size() + " " + entry.getKey() + " resources into fewer, right-sized instances",
                    consolidatedSaving
                ));
            }
        }

        return issues;
    }

    @Override
    public List<Issue> analyze(List<Issue> issues) {
        for (Issue issue : issues) {
            if (issue.getPotentialSavings() > 8000) {
                issue.setSeverity("HIGH");
            }
            if ("CONSOLIDATION_OPPORTUNITY".equals(issue.getType()) && issue.getPotentialSavings() > 10000) {
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
            String status;

            if ("CONSOLIDATION_OPPORTUNITY".equals(issue.getType())) {
                actionType = "CONSOLIDATE_RESOURCES";
                description = "Initiated resource consolidation workflow. " + issue.getSuggestedAction() + " | Potential saving: ₹" + String.format("%.0f", issue.getPotentialSavings());
                status = "PENDING_APPROVAL";
            } else {
                actionType = "OPTIMIZE_RESOURCE";
                description = "Recommended resource optimization: " + issue.getSuggestedAction() + " | Potential saving: ₹" + String.format("%.0f", issue.getPotentialSavings());
                status = issue.getPotentialSavings() > 5000 ? "PENDING_APPROVAL" : "EXECUTED";
            }

            actions.add(new ActionItem(
                "ACT-RO-" + (++actionCount),
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

            if ("CONSOLIDATION_OPPORTUNITY".equals(issue.getType())) {
                title = "Resource Consolidation Execution";
                steps = Arrays.asList(
                    "1. Audit current resource utilization across identified instances",
                    "2. Design consolidated architecture with right-sized capacity",
                    "3. Submit consolidation plan for infrastructure team review",
                    "4. Execute migration during maintenance window",
                    "5. Decommission redundant instances",
                    "6. Verify service levels post-consolidation for 7 days",
                    "7. Update billing and capacity tracking"
                );
                costMath = "Current Total Cost: ₹" + String.format("%.0f", issue.getPotentialSavings() / 0.7)
                    + " | Post-Consolidation Cost: ₹" + String.format("%.0f", issue.getPotentialSavings() / 0.7 - issue.getPotentialSavings())
                    + " | Migration Cost: ~₹" + String.format("%.0f", issue.getPotentialSavings() * 0.1)
                    + " | Net Annual Savings: ₹" + String.format("%.0f", issue.getPotentialSavings() * 12 * 0.9);
            } else {
                title = "Right-Size Underutilized Resource";
                steps = Arrays.asList(
                    "1. Review 30-day utilization metrics for resource",
                    "2. Identify appropriate right-sized tier/plan",
                    "3. Schedule downgrade during low-traffic window",
                    "4. Execute tier change and monitor for 48 hours",
                    "5. Confirm cost reduction in next billing cycle"
                );
                costMath = "Current Cost: ₹" + String.format("%.0f", issue.getPotentialSavings() / 0.7)
                    + " | Wasted Capacity: ₹" + String.format("%.0f", issue.getPotentialSavings())
                    + " | Right-Sized Cost: ₹" + String.format("%.0f", issue.getPotentialSavings() / 0.7 - issue.getPotentialSavings())
                    + " | Monthly Savings: ₹" + String.format("%.0f", issue.getPotentialSavings());
            }

            playbooks.add(new Playbook(
                "PB-RO-" + (++pbCount),
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
