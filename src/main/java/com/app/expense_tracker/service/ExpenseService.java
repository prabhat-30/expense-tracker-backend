package com.app.expense_tracker.service;

import com.app.expense_tracker.dto.analytics.CategoryTotalDTO;
import com.app.expense_tracker.dto.analytics.DashboardSummaryDTO;
import com.app.expense_tracker.dto.analytics.MonthlyExpenseDTO;
import com.app.expense_tracker.dto.ExpenseDTO;
import com.app.expense_tracker.dto.analytics.YearlyExpenseDTO;
import com.app.expense_tracker.entity.Budget;
import com.app.expense_tracker.entity.Expense;
import com.app.expense_tracker.entity.User;
import com.app.expense_tracker.exception.ResourceNotFoundException;
import com.app.expense_tracker.repository.BudgetRepository;
import com.app.expense_tracker.repository.ExpenseRepository;
import com.app.expense_tracker.repository.UserRepository;
import com.app.expense_tracker.security.SecuritySanitizer;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository repository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private RecurringExpenseService recurringExpenseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SecuritySanitizer sanitizer;

    private void checkBudgetThresholdAlerts(String username, String category) {
        Integer currentYear = Integer.valueOf(LocalDate.now().getYear());
        Integer currentMonth = Integer.valueOf(LocalDate.now().getMonthValue());

        budgetRepository.findByUserUsernameAndCategory(username, category).ifPresent(budget -> {
            double limitAmount = budget.getLimitAmount();
            if (limitAmount > 0) {
                List<Object[]> rows = repository.getCategoryWiseSpending(username, currentYear, currentMonth);
                double categorySpent = 0.0;

                for (Object[] row : rows) {
                    if (category.equalsIgnoreCase((String) row[0])) {
                        categorySpent = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                        break;
                    }
                }

                double consumptionPercentage = (categorySpent / limitAmount) * 100;
                String currentState = budget.getAlertState();

                if (consumptionPercentage >= 100.0 && !"EXCEEDED_SENT".equals(currentState)) {
                    User user = budget.getUser();
                    String subject = String.format("🚨 CRITICAL LIMIT BREACH: %s Budget Exceeded", category.toUpperCase());
                    String text = String.format(
                            "Dear %s,\n\n" +
                                    "Your monthly spending for the '%s' category has officially breached 100%% of your budget.\n\n" +
                                    "▪ Configured Monthly Limit: ₹%.2f\n" +
                                    "▪ Total Spent This Month: ₹%.2f\n\n" +
                                    "Inbox notifications for this category are now muted until next month.",
                            user.getName(), category, limitAmount, categorySpent
                    );

                    emailService.sendSimpleMessage(user.getEmail(), subject, text);

                    budget.setAlertState("EXCEEDED_SENT");
                    budgetRepository.save(budget);
                    System.out.println("📬 Final breach notification delivered. Muting inbox loops.");

                } else if (consumptionPercentage >= 90.0 && consumptionPercentage < 100.0 && currentState == null) {
                    User user = budget.getUser();
                    String subject = String.format("⚠️ Budget Warning: %s limit at %.1f%%", category.toUpperCase(), consumptionPercentage);
                    String text = String.format(
                            "Dear %s,\n\n" +
                                    "Your monthly spending for the '%s' category has reached %.1f%% of your target limit.\n\n" +
                                    "▪ Configured Monthly Limit: ₹%.2f\n" +
                                    "▪ Current Month Spending: ₹%.2f\n\n" +
                                    "You will receive exactly one more email if this category breaches 100%%.",
                            user.getName(), category, consumptionPercentage, limitAmount, categorySpent
                    );

                    emailService.sendSimpleMessage(user.getEmail(), subject, text);

                    budget.setAlertState("WARNING_SENT");
                    budgetRepository.save(budget);
                    System.out.println("📬 Initial 90% warning card delivered. State locked.");
                }
            }
        });
    }

    public Expense addExpense(@Valid ExpenseDTO dto, @Valid String username) {
        String safeTitle = sanitizer.sanitizeText(dto.getTitle());
        String safeCategory = sanitizer.sanitizeText(dto.getCategory());

        dto.setTitle(safeTitle);
        dto.setCategory(safeCategory);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Expense expense = new Expense();
        expense.setTitle(dto.getTitle());
        expense.setAmount(dto.getAmount());
        expense.setCategory(dto.getCategory());

        if (dto.getDate() == null) {
            expense.setDate(LocalDate.now());
        } else {
            expense.setDate(dto.getDate());
        }
        expense.setType(dto.getType());
        expense.setUser(user);

        expense.setRecurring(dto.isRecurring());
        expense.setFrequency(dto.getFrequency());
        expense.setNextDate(dto.getNextDate());
        expense.setIncludeSat(dto.isIncludeSat());
        expense.setIncludeSun(dto.isIncludeSun());

        Expense saved = repository.save(expense);

        String actionType = dto.isUndo() ? "UNDO_DELETE" : "CREATE_EXPENSE";
        String details = dto.isUndo() ? "Restored deleted expense: " : "Added " + saved.getType() + ": ";

        auditLogService.log(username, actionType, details + saved.getTitle());

        if ("EXPENSE".equalsIgnoreCase(saved.getType())) {
            checkBudgetThresholdAlerts(username, saved.getCategory());
        }

        return saved;
    }

    public List<Expense> addAllExpenses(@Valid List<ExpenseDTO> dtos, String username) {

        for (ExpenseDTO dto : dtos) {
            dto.setTitle(sanitizer.sanitizeText(dto.getTitle()));
            dto.setCategory(sanitizer.sanitizeText(dto.getCategory()));
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Expense> expenses = new ArrayList<>();

        for (ExpenseDTO dto : dtos) {
            Expense expense = new Expense();
            expense.setRecurring(dto.isRecurring());
            expense.setFrequency(dto.getFrequency());
            expense.setNextDate(dto.getNextDate());
            expense.setIncludeSat(dto.isIncludeSat());
            expense.setIncludeSun(dto.isIncludeSun());
            expense.setTitle(dto.getTitle());
            expense.setAmount(dto.getAmount());
            expense.setCategory(dto.getCategory());

            if (dto.getDate() == null) {
                expense.setDate(LocalDate.now());
            } else {
                expense.setDate(dto.getDate());
            }
            expense.setType(dto.getType());
            expense.setUser(user);
            expenses.add(expense);
        }

        List<Expense> savedList = repository.saveAll(expenses);

        auditLogService.log(username, "BULK_CREATE", "Imported " + savedList.size() + " records via bulk entry.");

        savedList.stream()
                .filter(e -> "EXPENSE".equalsIgnoreCase(e.getType()))
                .map(Expense::getCategory)
                .distinct()
                .forEach(cat -> checkBudgetThresholdAlerts(username, cat));

        return savedList;
    }

    public Page<Expense> getAllExpense(String username, Pageable pageable) {
        return repository.findByUserUsernameOrderByIdDesc(username, pageable);
    }

    public Expense getExpenseById(@Valid Long id, String username) {
        return repository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id:" + id));
    }

    public List<Expense> getRecentExpenses(String username) {
        return repository.findTop5ByUserUsernameOrderByIdDesc(username);
    }

    public void deleteExpense(Long id, String username) {
        Expense expense = repository
                .findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        auditLogService.log(username, "DELETE_EXPENSE",
                "Deleted " + expense.getType() + ": " + expense.getTitle() + " (Amount: " + expense.getAmount() + ")");

        repository.delete(expense);
    }

    @Transactional
    public void deleteAllExpenses(String username){
        auditLogService.log(username, "WIPE_DATA", "User requested permanent deletion of their personal expense history.");
        repository.deleteByByUserUsername(username);
    }

    public Expense updateExpense(Long id, @Valid ExpenseDTO updateExpense, String username) {

        String safeTitle = sanitizer.sanitizeText(updateExpense.getTitle());
        String safeCategory = sanitizer.sanitizeText(updateExpense.getCategory());

        updateExpense.setTitle(safeTitle);
        updateExpense.setCategory(safeCategory);

        Expense existing = repository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id:" + id));

        existing.setTitle(updateExpense.getTitle());
        existing.setAmount(updateExpense.getAmount());
        existing.setCategory(updateExpense.getCategory());
        existing.setType(updateExpense.getType());

        if (updateExpense.getDate() != null) {
            existing.setDate(updateExpense.getDate());
        }

        existing.setRecurring(updateExpense.isRecurring());
        existing.setFrequency(updateExpense.getFrequency());
        existing.setNextDate(updateExpense.getNextDate());
        existing.setIncludeSat(updateExpense.isIncludeSat());
        existing.setIncludeSun(updateExpense.isIncludeSun());

        Expense saved = repository.save(existing);

        String actionType = updateExpense.isUndo() ? "UNDO_STOP_REPEAT" : "UPDATE_EXPENSE";
        String details = updateExpense.isUndo() ? "Restored automation for: " : "Updated: ";

        auditLogService.log(username, actionType, details + saved.getTitle());

        if ("EXPENSE".equalsIgnoreCase(saved.getType())) {
            checkBudgetThresholdAlerts(username, saved.getCategory());
        }

        return saved;
    }

    public Page<Expense> getByCategory(String username, String category, Pageable pageable) {
        return repository.findByUserUsernameAndCategory(username, category, pageable);
    }

    public Page<Expense> getByTitle(String username, String title, Pageable pageable) {
        return repository.findByUserUsernameAndTitleContainingIgnoreCase(username, title, pageable);
    }

    public Page<Expense> getByAmount(String username, Double amount, Pageable pageable) {
        return repository.findByUserUsernameAndAmount(username, amount, pageable);
    }

    public Page<Expense> getByAmountGreaterThan(String username, Double amount, Pageable pageable) {
        return repository.findByUserUsernameAndAmountGreaterThan(username, amount, pageable);
    }

    public Page<Expense> getByTitleAndCategory(String username, String title, String category, Pageable pageable) {
        return repository.findByUserUsernameAndTitleAndCategory(username, title, category, pageable);
    }

    public List<Expense> getByDate(String username, LocalDate date) {
        return repository.findByUserUsernameAndDate(username, date);
    }

    public Page<Expense> getByDateBetween(String username, LocalDate start, LocalDate end, Pageable pageable) {
        return repository.findByUserUsernameAndDateBetween(username, start, end, pageable);
    }

    public Double getTotalExpense(String username) {
        return repository.getTotalExpense(username, null, null);
    }

    public Long getExpenseCount(String username) {
        return repository.getExpenseCount(username);
    }

    // 🌟 FIXED: Now correctly filters by BOTH username and transaction type
    public Optional<Expense> getHighestExpense(String username) {
        return repository.findTopByUserUsernameAndTypeOrderByAmountDesc(username, "EXPENSE");
    }

    public List<Expense> getRecentTransactions(String username) {
        return repository.findTop5ByUserUsernameOrderByDateDesc(username);
    }

    public List<CategoryTotalDTO> getCategoryTotals(String username) {
        return repository.getCategoryTotals(username);
    }

    public Double getAverageExpense(String username) {
        return repository.getAverageExpense(username);
    }

    public List<YearlyExpenseDTO> getYearlyTotals(String username) {
        return repository.getYearlyTotals(username);
    }

    public DashboardSummaryDTO getDashboardSummaryByYearAndMonth(String username, String yearStr, Integer month) {
        Integer year = (yearStr == null || yearStr.equals("ALL") || yearStr.isEmpty())
                ? null
                : Integer.parseInt(yearStr);

        Integer targetMonth = (year == null) ? null : month;

        Double totalExpense = repository.getTotalExpense(username, year, targetMonth);
        Double totalIncome = repository.getTotalIncome(username, year, targetMonth);
        Double balance = (totalIncome != null ? totalIncome : 0.0) - (totalExpense != null ? totalExpense : 0.0);

        Long count = repository.getExpenseCount(username);

        // 🌟 FIXED: Now restricts the search to the specific user session to prevent cross-account data leaks
        Expense highest = repository.findTopByUserUsernameAndTypeOrderByAmountDesc(username, "EXPENSE").orElse(null);

        List<Expense> recent = repository.findTop5ByUserUsernameOrderByIdDesc(username);

        List<Object[]> categoryRows = repository.getCategoryWiseSpending(username, year, targetMonth);
        Map<String, Double> categoryMap = categoryRows.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                        (existing, replacement) -> existing
                ));

        List<Budget> userBudgets = budgetRepository.findByUserUsername(username);
        Map<String, Double> budgetMap = userBudgets.stream()
                .collect(Collectors.toMap(
                        Budget::getCategory,
                        Budget::getLimitAmount,
                        (existing, replacement) -> existing
                ));

        return new DashboardSummaryDTO(
                totalExpense,
                totalIncome,
                balance,
                count,
                highest,
                recent,
                categoryMap,
                budgetMap
        );
    }

    public DashboardSummaryDTO getDashboardSummaryByYear(String username, String yearStr) {
        return getDashboardSummaryByYearAndMonth(username, yearStr, null);
    }

    public DashboardSummaryDTO getDashboardSummary(String username) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        return getDashboardSummaryByYearAndMonth(username, String.valueOf(currentYear), currentMonth);
    }

    public Map<String, Object> getOverviewSummary(String username, String yearStr) {
        Integer year = (yearStr == null || yearStr.equals("ALL") || yearStr.isEmpty())
                ? null
                : Integer.parseInt(yearStr);

        Map<String, Object> response = new java.util.HashMap<>();

        Double totalExpense = repository.getTotalExpense(username, year, null);
        Double totalIncome = repository.getTotalIncome(username, year, null);

        response.put("totalSpent", totalExpense != null ? totalExpense : 0.0);
        response.put("totalIncome", totalIncome != null ? totalIncome : 0.0);

        List<Object[]> categoryRows = repository.getCategoryWiseSpending(username, year, null);
        Map<String, Double> categoryMap = categoryRows.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                        (existing, replacement) -> existing
                ));
        response.put("categoryData", categoryMap);

        List<Budget> userBudgets = budgetRepository.findByUserUsername(username);
        Map<String, Double> budgetMap = userBudgets.stream()
                .collect(Collectors.toMap(
                        Budget::getCategory,
                        Budget::getLimitAmount,
                        (existing, replacement) -> existing
                ));
        response.put("budgets", budgetMap);

        return response;
    }

    public Page<Expense> searchAdvanced(String username, String category, String keyword, String type, Pageable pageable) {
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim() + "%"
                : null;

        return repository.searchAdvanced(username, category, searchKeyword, type, pageable);
    }

    public void setBudget(String username, String category, Double limit) {
        if (limit == null || limit <= 0) {
            throw new IllegalArgumentException("Validation Error: Monthly budget allocation limit must be greater than zero.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User session context not found."));

        Budget budget = budgetRepository.findByUserUsernameAndCategory(username, category)
                .orElseGet(() -> {
                    Budget newBudget = new Budget();
                    newBudget.setCategory(category);
                    newBudget.setUser(user);
                    return newBudget;
                });

        budget.setLimitAmount(limit);
        budget.setAlertState(null);

        budgetRepository.save(budget);

        auditLogService.log(username, "SET_BUDGET", "Set budget for " + category + " to ₹" + limit);
    }

    public Expense stopRecurring(Long id, String username) {
        Expense expense = repository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        expense.setRecurring(false);
        expense.setFrequency(null);
        expense.setNextDate(null);
        expense.setIncludeSat(false);
        expense.setIncludeSun(false);

        Expense saved = repository.save(expense);

        auditLogService.log(username, "STOP_RECURRING", "Disabled recurring automation for: " + saved.getTitle());

        return saved;
    }

    public List<Map<String, Object>> getMonthlyTotals(String username, String yearStr) {
        Integer year = (yearStr == null || yearStr.equals("ALL") || yearStr.isEmpty())
                ? null
                : Integer.parseInt(yearStr);

        List<Object[]> results = repository.getMonthlyAnalytics(username, year);
        List<Map<String, Object>> trendList = new java.util.ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> dataPoint = new java.util.HashMap<>();
            dataPoint.put("month", row[0]);
            dataPoint.put("total", row[1] != null ? row[1] : 0.0);
            trendList.add(dataPoint);
        }

        return trendList;
    }
}