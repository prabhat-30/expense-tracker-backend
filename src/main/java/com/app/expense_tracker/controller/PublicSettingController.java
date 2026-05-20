package com.app.expense_tracker.controller;

import com.app.expense_tracker.service.admin.SystemSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/settings")
public class PublicSettingController {

    private final SystemSettingService settingService;

    public PublicSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    // Exposes a safe, unprivileged endpoint for standard user interfaces
    @GetMapping
    public ResponseEntity<Map<String, String>> getActiveConfigurationMatrix() {
        Map<String, String> publicConfigs = new HashMap<>();

        // Grab values with safe hardcoded defaults if database rows are completely absent
        publicConfigs.put("currency", settingService.getSettingValue("DEFAULT_CURRENCY", "INR"));
        publicConfigs.put("maxFileSize", settingService.getSettingValue("MAX_FILE_SIZE_MB", "5"));
        publicConfigs.put("budgetThreshold", settingService.getSettingValue("BUDGET_ALERT_THRESHOLD", "85"));

        return ResponseEntity.ok(publicConfigs);
    }
}