package com.app.expense_tracker.controller.admin;

import com.app.expense_tracker.entity.SystemSetting;
import com.app.expense_tracker.service.admin.SystemSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/settings")
public class SystemSettingController {

    private final SystemSettingService settingService;

    public SystemSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    // GET /admin/settings
    @GetMapping
    public ResponseEntity<List<SystemSetting>> getSystemSettings() {
        return ResponseEntity.ok(settingService.getAllSettings());
    }

    // PUT /admin/settings/update
    @PutMapping("/update")
    public ResponseEntity<String> updateSystemSettings(
            @RequestBody List<SystemSetting> settings,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        settingService.updateSettings(settings, adminUsername);
        return ResponseEntity.ok("Global platform settings synchronized successfully.");
    }
}