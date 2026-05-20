package com.app.expense_tracker.service;

import com.app.expense_tracker.entity.AuditLog;
import com.app.expense_tracker.repository.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository) {

        this.auditLogRepository =
                auditLogRepository;
    }

    public void log(String username,
                    String action,
                    String details) {

        AuditLog auditLog = new AuditLog();

        auditLog.setUsername(username);

        auditLog.setAction(action);

        auditLog.setDetails(details);

        auditLog.setTimestamp(java.time.LocalDateTime.now());

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            auditLog.setIpAddress(request.getRemoteAddr());
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        }

        auditLogRepository.save(auditLog);
    }
}