package com.app.expense_tracker.controller;

import com.app.expense_tracker.entity.AuditLog;
import com.app.expense_tracker.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/security-logs")
public class UserAuditController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<Page<AuditLog>> getMySecurityLogs(Authentication authentication, Pageable pageable) {
        String username = authentication.getName();
        // This ensures a user can NEVER see another user's logs
        return ResponseEntity.ok(auditLogRepository.findByUsernameOrderByTimestampDesc(username, pageable));
    }
}