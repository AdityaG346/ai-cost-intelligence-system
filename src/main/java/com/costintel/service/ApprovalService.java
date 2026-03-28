package com.costintel.service;

import com.costintel.models.ApprovalRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ApprovalService {

    private final List<ApprovalRequest> approvalQueue = new ArrayList<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void addRequest(ApprovalRequest request) {
        approvalQueue.add(request);
    }

    public List<ApprovalRequest> getAllRequests() {
        return new ArrayList<>(approvalQueue);
    }

    public List<ApprovalRequest> getPendingRequests() {
        List<ApprovalRequest> pending = new ArrayList<>();
        for (ApprovalRequest r : approvalQueue) {
            if ("PENDING".equals(r.getStatus())) {
                pending.add(r);
            }
        }
        return pending;
    }

    public Optional<ApprovalRequest> approveRequest(String id) {
        for (ApprovalRequest r : approvalQueue) {
            if (r.getId().equals(id) && "PENDING".equals(r.getStatus())) {
                r.setStatus("APPROVED");
                r.setResolvedAt(LocalDateTime.now().format(FMT));
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    public Optional<ApprovalRequest> rejectRequest(String id) {
        for (ApprovalRequest r : approvalQueue) {
            if (r.getId().equals(id) && "PENDING".equals(r.getStatus())) {
                r.setStatus("REJECTED");
                r.setResolvedAt(LocalDateTime.now().format(FMT));
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    public void clear() {
        approvalQueue.clear();
    }
}
