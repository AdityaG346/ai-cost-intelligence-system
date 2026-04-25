package com.costintel.agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.costintel.models.ActionItem;
import com.costintel.models.Issue;
import com.costintel.models.Playbook;
import com.costintel.models.Transaction;

@Component
public class SpendIntelligenceAgent implements Agent {

    @Override
    public String getName() {
        return "Spend Intelligence Agent";
    }

    @Override
    public List<Issue> detect(List<Transaction> transactions) {
        List<Issue> issues = new ArrayList<>();
        int issueCount = 0;

        // 1. Detect duplicate payments (same vendor + same amount + same date)
        Map<String, List<Transaction>> grouped = new HashMap<>();
        for (Transaction t : transactions) {
            String key = t.getVendor() + "|" + t.getAmount() + "|" + t.getDate();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }
        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (int i = 1; i < entry.getValue().size(); i++) {
                    Transaction dup = entry.getValue().get(i);
                    issues.add(new Issue(
                        "SI-" + (++issueCount),
                        "DUPLICATE_PAYMENT",
                        "HIGH",
                        "Duplicate payment detected: ₹" + dup.getAmount() + " to " + dup.getVendor() + " on " + dup.getDate(),
                        getName(),
                        dup.getId(),
                        "Flag and reverse duplicate payment",
                        dup.getAmount()
                    ));
                }
            }
        }

        // 2. Detect abnormal spending (spike > 2x average for that vendor)
        Map<String, List<Double>> vendorAmounts = new HashMap<>();
        for (Transaction t : transactions) {
            vendorAmounts.computeIfAbsent(t.getVendor(), k -> new ArrayList<>()).add(t.getAmount());
        }
        for (Transaction t : transactions) {
            List<Double> amounts = vendorAmounts.get(t.getVendor());
            if (amounts.size() >= 2) {
                double avg = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                if (t.getAmount() > avg * 2 && t.getAmount() > 1000) {
                    boolean alreadyFlagged = issues.stream()
                        .anyMatch(i -> i.getAffectedTransactionId() != null && i.getAffectedTransactionId().equals(t.getId()));
                    if (!alreadyFlagged) {
                        double excess = t.getAmount() - avg;
                        issues.add(new Issue(
                            "SI-" + (++issueCount),
                            "SPENDING_ANOMALY",
                            "MEDIUM",
                            "Abnormal spending spike: ₹" + t.getAmount() + " to " + t.getVendor() + " (avg: ₹" + String.format("%.0f", avg) + ")",
                            getName(),
                            t.getId(),
                            "Review and justify the spending spike",
                            excess
                        ));
                    }
                }
            }
        }

        // 3. Detect recurring expenses (same vendor, multiple entries)
        for (Map.Entry<String, List<Double>> entry : vendorAmounts.entrySet()) {
            if (entry.getValue().size() >= 3) {
                double total = entry.getValue().stream().mapToDouble(Double::doubleValue).sum();
                if (total > 5000) {
                    issues.add(new Issue(
                        "SI-" + (++issueCount),
                        "RECURRING_EXPENSE",
                        "LOW",
                        "Recurring expense pattern: " + entry.getValue().size() + " payments to " + entry.getKey() + " totaling ₹" + String.format("%.0f", total),
                        getName(),
                        null,
                        "Review necessity of recurring payments to " + entry.getKey(),
                        total * 0.15
                    ));
                }
            }
        }

        // 4. Rate optimization detection — compare vendor rates across similar categories
        Map<String, List<Transaction>> categoryTxns = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getCategory() != null) {
                categoryTxns.computeIfAbsent(t.getCategory(), k -> new ArrayList<>()).add(t);
            }
        }
        for (Map.Entry<String, List<Transaction>> entry : categoryTxns.entrySet()) {
            List<Transaction> txns = entry.getValue();
            if (txns.size() >= 2) {
                double minRate = txns.stream().mapToDouble(Transaction::getAmount).min().orElse(0);
                double maxRate = txns.stream().mapToDouble(Transaction::getAmount).max().orElse(0);
                if (maxRate > minRate * 1.5 && maxRate > 5000) {
                    Transaction expensive = txns.stream()
                        .max(Comparator.comparingDouble(Transaction::getAmount)).orElse(null);
                    if (expensive != null) {
                        boolean alreadyFlagged = issues.stream()
                            .anyMatch(i -> i.getAffectedTransactionId() != null && i.getAffectedTransactionId().equals(expensive.getId()));
                        if (!alreadyFlagged) {
                            double potentialSaving = maxRate - minRate;
                            issues.add(new Issue(
                                "SI-" + (++issueCount),
                                "RATE_OPTIMIZATION",
                                "MEDIUM",
                                "Rate optimization: " + entry.getKey() + " category shows ₹" + String.format("%.0f", minRate) + " to ₹" + String.format("%.0f", maxRate) + " spread. Negotiate with " + expensive.getVendor(),
                                getName(),
                                expensive.getId(),
                                "Negotiate rate with " + expensive.getVendor() + " or switch to lower-cost vendor in " + entry.getKey(),
                                potentialSaving * 0.3
                            ));
                        }
                    }
                }
            }
        }

        return issues;
    }

    @Override
    public List<Issue> analyze(List<Issue> issues) {
        for (Issue issue : issues) {
            if (issue.getPotentialSavings() > 10000) {
                issue.setSeverity("CRITICAL");
            } else if (issue.getPotentialSavings() > 5000) {
                issue.setSeverity("HIGH");
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
                case "DUPLICATE_PAYMENT":
                    actionType = "FLAG_DUPLICATE";
                    description = "Flagged duplicate payment and initiated reversal process. " + issue.getDescription();
                    break;
                case "SPENDING_ANOMALY":
                    actionType = "ALERT_ANOMALY";
                    description = "Triggered spending anomaly alert for management review. " + issue.getDescription();
                    status = "PENDING_REVIEW";
                    break;
                case "RECURRING_EXPENSE":
                    actionType = "REVIEW_SUBSCRIPTION";
                    description = "Suggested cancellation review for recurring expense. " + issue.getDescription();
                    break;
                case "RATE_OPTIMIZATION":
                    actionType = "NEGOTIATE_RATE";
                    description = "Initiated vendor rate renegotiation workflow. " + issue.getDescription();
                    status = "PENDING_APPROVAL";
                    break;
                default:
                    actionType = "GENERAL_FLAG";
                    description = issue.getSuggestedAction();
            }
            actions.add(new ActionItem(
                "ACT-SI-" + (++actionCount),
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
                case "DUPLICATE_PAYMENT":
int idx = issue.getDescription().indexOf("to ");
String vendorPart = (idx != -1 && idx + 3 < issue.getDescription().length())
        ? issue.getDescription().substring(idx + 3)
        : "Vendor";

title = "Reverse Duplicate Payment — " + vendorPart;                    steps = Arrays.asList(
                        "1. Verify payment duplication in ERP system",
                        "2. Flag transaction " + issue.getAffectedTransactionId() + " as duplicate",
                        "3. Initiate reversal request with finance team",
                        "4. Notify vendor of duplicate and request credit note",
                        "5. Update ledger and confirm reversal within 48 hours"
                    );
                    costMath = "Duplicate Amount: ₹" + String.format("%.0f", issue.getPotentialSavings())
                        + " | Recovery Rate: 100% | Net Savings: ₹" + String.format("%.0f", issue.getPotentialSavings());
                    break;
                case "SPENDING_ANOMALY":
                    title = "Investigate Spending Spike";
                    steps = Arrays.asList(
                        "1. Pull transaction history for vendor (last 6 months)",
                        "2. Compare current amount vs. historical average",
                        "3. Request justification from department head",
                        "4. If unjustified, cap future spend at historical avg + 20%",
                        "5. Set up automated alerts for future spikes"
                    );
                    costMath = "Spike Excess: ₹" + String.format("%.0f", issue.getPotentialSavings())
                        + " | Recoverable: ~60% | Projected Savings: ₹" + String.format("%.0f", issue.getPotentialSavings() * 0.6);
                    break;
                case "RATE_OPTIMIZATION":
                    title = "Vendor Rate Renegotiation";
                    steps = Arrays.asList(
                        "1. Benchmark current vendor rates against market rates",
                        "2. Identify 2-3 alternative vendors for competitive quotes",
                        "3. Prepare negotiation brief with volume commitments",
                        "4. Schedule vendor negotiation meeting",
                        "5. Execute new contract at optimized rate"
                    );
                    costMath = "Current Premium: ₹" + String.format("%.0f", issue.getPotentialSavings() / 0.3)
                        + " | Target Reduction: 30% | Projected Savings: ₹" + String.format("%.0f", issue.getPotentialSavings());
                    break;
                default:
                    title = "Review & Optimize — " + issue.getType().replace("_", " ");
                    steps = Arrays.asList(
                        "1. Review flagged transaction details",
                        "2. Assess potential for cost reduction",
                        "3. Implement approved optimization"
                    );
                    costMath = "Estimated Savings: ₹" + String.format("%.0f", issue.getPotentialSavings());
            }

            playbooks.add(new Playbook(
                "PB-SI-" + (++pbCount),
                title,
                getName(),
                issue.getId(),
                steps,
                costMath,
                issue.getPotentialSavings(),
                issue.getPotentialSavings() > 5000 ? "GENERATED" : "GENERATED",
                issue.getSeverity()
            ));
        }
        return playbooks;
    }
}
