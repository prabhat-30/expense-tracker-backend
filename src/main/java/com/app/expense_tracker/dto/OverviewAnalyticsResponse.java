package com.app.expense_tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewAnalyticsResponse {
    private Map<String, Double> topExpensesByCategory; // Top categories + "Others"
    private double totalIncomeForMonth;
    private double totalExpenseForMonth;
    private Map<String, BudgetProgressDTO> budgetProgress; // Category -> Spent vs Limit
}