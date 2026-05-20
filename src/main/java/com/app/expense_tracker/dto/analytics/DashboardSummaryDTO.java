package com.app.expense_tracker.dto.analytics;

import com.app.expense_tracker.entity.Expense;
import lombok.AllArgsConstructor; // Ensure this import is here
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor // This generates the constructor with 6 arguments
public class DashboardSummaryDTO {
    private Double totalSpent;
    private Double totalIncome;
    private Double totalBalance;
    private Long transactionCount;
    private Expense highestExpense;
    private List<Expense> recentTransactions;
    private Map<String, Double> categoryData; // Map of Category Name -> Total Amount
    private Map<String, Double> budgets;

    public DashboardSummaryDTO(Double totalExpense, Double totalIncome, Double balance, Long count, Optional<Expense> highest, List<Expense> recent, Map<String, Double> categoryMap, Map<String, Double> budgetMap) {
    }
}