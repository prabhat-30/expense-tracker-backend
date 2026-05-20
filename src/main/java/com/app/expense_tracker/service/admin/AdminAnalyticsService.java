package com.app.expense_tracker.service.admin;

import com.app.expense_tracker.dto.AdminAnalyticsDTO;
import com.app.expense_tracker.entity.Expense;
import com.app.expense_tracker.repository.ExpenseRepository;
import com.app.expense_tracker.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;

    public AdminAnalyticsService(
            UserRepository userRepository,
            ExpenseRepository expenseRepository
    ) {
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Refactored Core KPI Calculation Engine matching Step 1.2
     * Computes platform aggregates dynamically using optional timeline scoping boundaries.
     */
    public AdminAnalyticsDTO getAnalytics(Integer year, Integer month) {

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long blockedUsers = userRepository.countByIsActiveFalse();

        // 🌟 1. Compute dynamic platform counts and aggregates using database filters
        long totalTransactions = expenseRepository.countByFilters(null, year, month);

        long totalExpenses = expenseRepository.countByFilters("EXPENSE", year, month);

        double totalExpenseAmount = expenseRepository.sumAmountByFilters("EXPENSE", year, month);

        double totalIncomeAmount = expenseRepository.sumAmountByFilters("INCOME", year, month);

        // 🌟 2. Calculate platform Liquidity Room: "Income - Spent"
        double totalNetBalance = totalIncomeAmount - totalExpenseAmount;

        // 🌟 3. Extract top used operational category inside defined timeframe constraints
        String mostUsedCategory = expenseRepository.findTopCategoryByFilters(year, month);
        if (mostUsedCategory == null) {
            mostUsedCategory = "None";
        }

        // 🌟 4. Trend Curve Calculations (Yearly scope context defaults safely)
        int evaluationYear = (year != null) ? year : LocalDate.now().getYear();
        double currentYearExpense = getYearExpense(evaluationYear);
        double previousYearExpense = getYearExpense(evaluationYear - 1);

        double expenseGrowth = calculateGrowth(currentYearExpense, previousYearExpense);

        return AdminAnalyticsDTO.builder()
                .totalUsers(totalUsers)
                .totalExpenses(totalExpenses)
                .activeUsers(activeUsers)
                .blockedUsers(blockedUsers)
                .mostUsedCategory(mostUsedCategory)
                .totalExpenseAmount(totalExpenseAmount)   // Sent to Total Spent Card
                .totalIncomeAmount(totalIncomeAmount)     // Sent to Total Income Card
                .totalTransactions(totalTransactions)     // Sent to Total Transactions Card
                .totalNetBalance(totalNetBalance)         // Sent to Net Balance Card
                .currentYearExpense(currentYearExpense)
                .previousYearExpense(previousYearExpense)
                .expenseGrowth(expenseGrowth)
                .build();
    }

    /**
     * Fetches historical benchmarks strictly matching true platform expense operations
     */
    public double getYearExpense(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        List<Expense> expenses = expenseRepository.findByDateBetween(start, end);

        // 🌟 FIXED: Added filters to prevent income entries from corrupting your annual baseline curve
        return expenses.stream()
                .filter(e -> "EXPENSE".equalsIgnoreCase(e.getType()))
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    public double calculateGrowth(double currentYear, double previousYear) {
        if (previousYear == 0) {
            return 0.0;
        }
        return ((currentYear - previousYear) / previousYear) * 100;
    }
}