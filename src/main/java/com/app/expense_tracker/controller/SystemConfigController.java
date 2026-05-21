package com.app.expense_tracker.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemConfigController {

    // Read the database URL to dynamically figure out if it's MySQL or Postgres
    @Value("${spring.datasource.url:}")
    private String dbUrl;

    // Read the Resend key to dynamically figure out if we are using HTTP or SMTP
    @Value("${app.email.resend.api-key:}")
    private String resendApiKey;

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getSystemStatus() {
        Map<String, String> status = new HashMap<>();

        // 1. Determine Database Engine
        if (dbUrl.toLowerCase().contains("postgresql")) {
            status.put("dbType", "POSTGRESQL");
            status.put("dbName", "PostgreSQL Database");
            status.put("dbSubtext", "Connection verification query loop: SELECT 1");
        } else {
            status.put("dbType", "MYSQL");
            status.put("dbName", "MySQL Database");
            status.put("dbSubtext", "Connection verification query loop: isValid()");
        }

        // 2. Determine Email Infrastructure
        if (resendApiKey != null && !resendApiKey.trim().isEmpty()) {
            status.put("emailType", "HTTP");
            status.put("emailName", "Resend HTTP API");
            status.put("emailSubtext", "Target Node: api.resend.com:443");
            status.put("emailBadge", "ONLINE");
        } else {
            status.put("emailType", "SMTP");
            status.put("emailName", "Gmail SMTP Server");
            status.put("emailSubtext", "Target Relay Port Node: smtp.gmail.com:587");
            status.put("emailBadge", "OFFLINE"); // Will show as offline if falling back to SMTP on Render
        }

        return ResponseEntity.ok(status);
    }
}