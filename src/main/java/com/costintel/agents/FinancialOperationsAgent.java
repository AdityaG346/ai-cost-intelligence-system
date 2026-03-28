package com.costintel.agents;

import com.costintel.models.ActionItem;
import com.costintel.models.Issue;
import com.costintel.models.Playbook;
import com.costintel.models.Transaction;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FinancialOperationsAgent implements Agent {

    @Override
    public String getName() {
        return "Financial Operations Agent";
    }

    @Override
    public List<Issue> detect(List<Transaction> transactions) {
        List<Issue> issues = new ArrayList<>();
        int issueCount = 0;

        for (Transaction t : transactions) {
            if (t.getExpectedAmount() > 0 && t.getActualAmount() > 0) {
                double diff = Math.abs(t.getExpectedAmount() - t.getActualAmount());
                if (diff > 0.01) {
                    String direction = t.getActualAmount() > t.getExpectedAmount() ? "OVERCHARGE" : "UNDERCHARGE";
                    double variance = (diff / t.getExpectedAmount()) * 100;

                    // Root-cause attribution
                    String rootCause;
                    if (variance > 15) {
                        rootCause = "CONTRACT_DEVIATION";
                    } else if (variance > 5) {
                        rootCause = "PRICING_ERROR";
                    } else {
                        rootCause = "QUANTITY_DISCREPANCY";
                    }

                    String severity;
                    if (diff > 5000) severity = "CRITICAL";
                    else if (diff > 1000) severity = "HIGH";
                    else severity = "MEDIUM";

                    issues.add(new Issue(
                        "FO-" + (++issueCount),
                        "TRANSACTION_MISMATCH",
                        severity,
                        direction + ": " + t.getVendor() + " expected ₹" + t.getExpectedAmount()
                            + " but actual ₹" + t.getActualAmount()
                            + " (variance: " + String.format("%.1f", variance) + "%, root cause: " + rootCause.replace("_", " ") + ")",
                        getName(),
                        t.getId(),
                        "Reconcile mismatch and " + (direction.equals("OVERCHARGE") ? "request refund" : "investigate undercharge")
                            + ". Root cause: " + rootCause.replace("_", " "),
                        direction.equals("OVERCHARGE") ? diff : 0
                    ));
                }
            }
        }

        // Variance analysis summary — detect systemic overbilling patterns per vendor
        Map<String, List<Double>> vendorVariances = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getExpectedAmount() > 0 && t.getActualAmount() > 0) {
                double varPct = ((t.getActualAmount() - t.getExpectedAmount()) / t.getExpectedAmount()) * 100;
                vendorVariances.computeIfAbsent(t.getVendor(), k -> new ArrayList<>()).add(varPct);
            }
        }
        for (Map.Entry<String, List<Double>> entry : vendorVariances.entrySet()) {
            if (entry.getValue().size() >= 2) {
                double avgVariance = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
                if (avgVariance > 3) {
                    issues.add(new Issue(
                        "FO-" + (++issueCount),
                        "SYSTEMIC_OVERBILLING",
                        "HIGH",
                        "Systemic overbilling: " + entry.getKey() + " averages +" + String.format("%.1f", avgVariance)
                            + "% above expected across " + entry.getValue().size() + " transactions. Root cause: systematic pricing error",
                        getName(),
                        null,
                        "Audit all transactions with " + entry.getKey() + " and renegotiate contract terms",
                        0
                    ));
                }
            }
        }

        return issues;
    }

    @Override
    public List<Issue> analyze(List<Issue> issues) {
        for (Issue issue : issues) {
            if (issue.getPotentialSavings() > 5000) {
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

            if ("SYSTEMIC_OVERBILLING".equals(issue.getType())) {
                actionType = "VENDOR_AUDIT";
                description = "Initiated full vendor audit and contract review. " + issue.getDescription();
                status = "PENDING_APPROVAL";
            } else {
                actionType = "RECONCILE_TRANSACTION";
                description = "Financial reconciliation action: " + issue.getSuggestedAction() + " | " + issue.getDescription();
                status = issue.getPotentialSavings() > 3000 ? "PENDING_APPROVAL" : "EXECUTED";
            }

            actions.add(new ActionItem(
                "ACT-FO-" + (++actionCount),
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

            if ("SYSTEMIC_OVERBILLING".equals(issue.getType())) {
                title = "Vendor Audit & Contract Renegotiation";
                steps = Arrays.asList(
                    "1. Pull all transactions for flagged vendor (last 12 months)",
                    "2. Calculate cumulative overcharge amount",
                    "3. Cross-reference with contract terms and agreed pricing",
                    "4. Prepare audit report with variance breakdown",
                    "5. Schedule vendor meeting with procurement team",
                    "6. Negotiate credit note for past overcharges",
                    "7. Amend contract with corrected pricing and penalty clauses"
                );
                costMath = "Systematic Variance: " + issue.getDescription().substring(issue.getDescription().indexOf("averages"))
                    + " | Estimated Annual Overcharge: requires full audit";
            } else {
                title = "Transaction Reconciliation & Recovery";
                steps = Arrays.asList(
                    "1. Verify expected vs. actual amounts against PO/invoice",
                    "2. Identify root cause: " + (issue.getDescription().contains("CONTRACT_DEVIATION") ? "contract deviation" :
                        issue.getDescription().contains("PRICING_ERROR") ? "pricing error" : "quantity discrepancy"),
                    "3. " + (issue.getPotentialSavings() > 0 ? "Submit refund request to vendor" : "Document undercharge for internal review"),
                    "4. Update reconciliation ledger",
                    "5. Flag vendor for enhanced monitoring on future transactions"
                );
                costMath = "Mismatch Amount: ₹" + String.format("%.0f", issue.getPotentialSavings())
                    + " | Recovery Rate: " + (issue.getPotentialSavings() > 3000 ? "95%" : "100%")
                    + " | Expected Recovery: ₹" + String.format("%.0f", issue.getPotentialSavings() * (issue.getPotentialSavings() > 3000 ? 0.95 : 1.0))
                    + " | Close Cycle Reduction: ~2 days";
            }

            playbooks.add(new Playbook(
                "PB-FO-" + (++pbCount),
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
