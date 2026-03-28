package com.costintel.utils;

import com.costintel.models.AuditEntry;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AuditLogger {

    private final List<AuditEntry> auditLog = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger stepCounter = new AtomicInteger(0);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void log(String agent, String message) {
        AuditEntry entry = new AuditEntry(
            LocalDateTime.now().format(FMT),
            stepCounter.incrementAndGet(),
            agent,
            message
        );
        auditLog.add(entry);
    }

    public List<AuditEntry> getAuditLog() {
        return new ArrayList<>(auditLog);
    }

    public void clear() {
        auditLog.clear();
        stepCounter.set(0);
    }
}
