package com.app.expense_tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetProgressDTO {
    private double spent;       // Total spent in this category for the month
    private double limitAmount; // The registered budget limit (can be null or 0 if unset)
    private double percentage;  // (spent / limitAmount) * 100 for easy frontend rendering
}