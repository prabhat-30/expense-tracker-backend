package com.app.expense_tracker.config;

import com.app.expense_tracker.entity.SystemSetting;
import com.app.expense_tracker.repository.SystemSettingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SystemSettingsInitializer implements CommandLineRunner {

    private final SystemSettingRepository settingRepository;

    public SystemSettingsInitializer(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Override
    public void run(String... args) {
        // Seed our 3 baseline controls
        initializeSetting("DEFAULT_CURRENCY", "INR", "The base currency code utilized for platform localization layouts.");
        initializeSetting("MAX_FILE_SIZE_MB", "5", "Maximum permitted file allocation boundary for transaction receipt image attachments.");
        initializeSetting("BUDGET_ALERT_THRESHOLD", "85", "The percentage execution limit where user budget consumption warning states trigger active color updates.");
    }

    private void initializeSetting(String key, String defaultValue, String description) {
        if (settingRepository.findByKey(key).isEmpty()) {
            SystemSetting setting = new SystemSetting(key, defaultValue, description);
            settingRepository.save(setting);
            System.out.println("SYSTEM CONFIG INITIALIZED: [" + key + " -> " + defaultValue + "]");
        }
    }
}