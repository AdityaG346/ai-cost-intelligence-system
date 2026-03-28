package com.costintel.agents;

import com.costintel.models.ActionItem;
import com.costintel.models.Issue;
import com.costintel.models.Playbook;
import com.costintel.models.Transaction;
import java.util.ArrayList;
import java.util.List;

public interface Agent {
    String getName();
    List<Issue> detect(List<Transaction> transactions);
    List<Issue> analyze(List<Issue> issues);
    List<ActionItem> act(List<Issue> issues);

    default List<Playbook> generatePlaybooks(List<Issue> issues) {
        return new ArrayList<>();
    }
}
