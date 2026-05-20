package com.app.expense_tracker.controller.admin;

import com.app.expense_tracker.dto.AdminAnalyticsDTO;
import com.app.expense_tracker.service.admin.AdminAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(
            AdminAnalyticsService adminAnalyticsService
    ) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @GetMapping
    public ResponseEntity<AdminAnalyticsDTO> analytics(
            @RequestParam(value = "year", required = false) String yearStr,
            @RequestParam(value = "month", required = false) String monthStr) {

        // Safely parse "ALL" strings or null boundaries into clean integer parameters for the DB
        Integer year = (yearStr == null || "ALL".equalsIgnoreCase(yearStr) || yearStr.trim().isEmpty())
                ? null
                : Integer.parseInt(yearStr.trim());

        Integer month = (monthStr == null || "ALL".equalsIgnoreCase(monthStr) || monthStr.trim().isEmpty())
                ? null
                : Integer.parseInt(monthStr.trim());

        return ResponseEntity.ok(
                adminAnalyticsService.getAnalytics(year, month)
        );
    }
}