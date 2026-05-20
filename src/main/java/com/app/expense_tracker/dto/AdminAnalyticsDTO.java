package com.app.expense_tracker.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAnalyticsDTO {

    private long totalUsers;

    private long totalExpenses;

    private long totalTransactions;

    private long activeUsers;

    private long blockedUsers;

    private String mostUsedCategory;

    private double currentYearExpense;

    private double previousYearExpense;

    private double expenseGrowth;

    private double totalExpenseAmount;   // 💸 Total Spent
    private double totalIncomeAmount;    // 🪙 Total Income
    private double totalNetBalance;     // ⚖️ Net Balance
}