package com.app.expense_tracker.service.admin;

import com.app.expense_tracker.entity.SystemSetting;
import com.app.expense_tracker.repository.SystemSettingRepository;
import com.app.expense_tracker.service.AuditLogService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SystemSettingService {

    private final SystemSettingRepository settingRepository;
    private final AuditLogService auditLogService;

    public SystemSettingService(SystemSettingRepository settingRepository, AuditLogService auditLogService) {
        this.settingRepository = settingRepository;
        this.auditLogService = auditLogService;
    }

    // Pull all rules to display on the Admin dashboard
    public List<SystemSetting> getAllSettings() {
        return settingRepository.findAll();
    }

    // Fallback tool: fetches a setting, or uses a default value if missing
    public String getSettingValue(String key, String defaultValue) {
        return settingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }

    // Save the settings updated from the web interface
    public void updateSettings(List<SystemSetting> updatedSettings, String adminUsername) {
        for (SystemSetting incoming : updatedSettings) {
            SystemSetting existing = settingRepository.findByKey(incoming.getKey())
                    .orElseGet(() -> {
                        SystemSetting newSetting = new SystemSetting();
                        newSetting.setKey(incoming.getKey());
                        return newSetting;
                    });

            String oldValue = existing.getValue();
            existing.setValue(incoming.getValue());
            if (incoming.getDescription() != null) {
                existing.setDescription(incoming.getDescription());
            }

            settingRepository.save(existing);

            // Trigger an automatic security audit entry if a setting changed
            if (oldValue == null || !oldValue.equals(incoming.getValue())) {
                auditLogService.log(
                        adminUsername,
                        "SYSTEM_CONFIG_MUTATED",
                        "Updated setting [" + incoming.getKey() + "] from '" + oldValue + "' to '" + incoming.getValue() + "'"
                );
            }
        }
    }
}