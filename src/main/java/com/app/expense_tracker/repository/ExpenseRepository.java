package com.app.expense_tracker.repository;

import com.app.expense_tracker.dto.analytics.YearlyExpenseDTO;
import com.app.expense_tracker.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.expense_tracker.dto.analytics.CategoryTotalDTO;
import com.app.expense_tracker.dto.analytics.MonthlyExpenseDTO;
import com.app.expense_tracker.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Expense related basic search
    Page<Expense> findByCategory(String category, Pageable pageable);
    Page<Expense> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Expense> findByAmount(Double amount, Pageable pageable);
    Page<Expense> findByTitleAndCategory(String title, String category, Pageable pageable);
    Page<Expense> findByAmountGreaterThan(Double amount, Pageable pageable);
    List<Expense> findByDate(LocalDate date);
    Page<Expense> findByDateBetween(LocalDate start, LocalDate end, Pageable pageable);

    // User related baseline search
    Page<Expense> findByUserUsername(String username, Pageable pageable);
    Optional<Expense> findByIdAndUserUsername(Long id, String username);
    Page<Expense> findByUserUsernameAndCategory(String username, String category, Pageable pageable);
    Page<Expense> findByUserUsernameAndTitleContainingIgnoreCase(String username, String title, Pageable pageable);
    Page<Expense> findByUserUsernameAndAmount(String username, Double amount, Pageable pageable);
    Page<Expense> findByUserUsernameAndTitleAndCategory(String username, String title, String category, Pageable pageable);
    Page<Expense> findByUserUsernameAndAmountGreaterThan(String username, Double amount, Pageable pageable);
    List<Expense> findByUserUsernameAndDate(String username, LocalDate date);
    Page<Expense> findByUserUsernameAndDateBetween(String username, LocalDate start, LocalDate end, Pageable pageable);

    // Adding "OrderByIdDesc" ensures the newest entries (highest IDs) come first
    Page<Expense> findByUserUsernameOrderByIdDesc(String username, Pageable pageable);
    // 🌟 PHASE 3 COMPLIANCE: Backlog-safe tracking method (Handles server downtime skips safely)
    @Query("SELECT e FROM Expense e WHERE e.recurring = true AND e.nextDate <= :today")
    List<Expense> findPendingRecurringExpenses(@Param("today") LocalDate today);

    // =========================================================================
    // UPDATED OVERVIEW SUMMARY OPERATIONS (SCOPED DYNAMICALLY TO YEAR & MONTH)
    // =========================================================================

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.username = :username AND e.type = 'EXPENSE' " +
            "AND (:year IS NULL OR YEAR(e.date) = :year) " +
            "AND (:month IS NULL OR MONTH(e.date) = :month)")
    Double getTotalExpense(@Param("username") String username, @Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.username = :username AND e.type = 'INCOME' " +
            "AND (:year IS NULL OR YEAR(e.date) = :year) " +
            "AND (:month IS NULL OR MONTH(e.date) = :month)")
    Double getTotalIncome(@Param("username") String username, @Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user.username = :username AND e.type = 'EXPENSE' " +
            "AND (:year IS NULL OR YEAR(e.date) = :year) " +
            "AND (:month IS NULL OR MONTH(e.date) = :month) " +
            "GROUP BY e.category")
    List<Object[]> getCategoryWiseSpending(@Param("username") String username, @Param("year") Integer year, @Param("month") Integer month);


    // =========================================================================
    // GENERAL METRICS & MACRO STATISTICS
    // =========================================================================

    // Total Count
    @Query("SELECT COUNT(e) FROM Expense e WHERE e.user.username = :username")
    Long getExpenseCount(@Param("username") String username);

    // Highest Expense
    Optional <Expense> findTopByUserUsernameAndTypeOrderByAmountDesc(String username, String type);

    // Recent Transactions
    List<Expense> findTop5ByUserUsernameOrderByDateDesc(String username);

    // Category Total DTO Projection (Used for distinct reporting metrics)
    @Query("SELECT new com.app.expense_tracker.dto.analytics.CategoryTotalDTO(e.category, SUM(e.amount)) " +
            "FROM Expense e WHERE e.user.username = :username GROUP BY e.category")
    List<CategoryTotalDTO> getCategoryTotals(@Param("username") String username);

    // Monthly Analytics Time-Series (Includes dynamic year grouping logic)
    @Query("SELECT MONTH(e.date), SUM(e.amount) FROM Expense e " +
            "WHERE e.user.username = :username " +
            "AND (:year IS NULL OR YEAR(e.date) = :year) " +
            "GROUP BY MONTH(e.date) ORDER BY MONTH(e.date)")
    List<Object[]> getMonthlyAnalytics(@Param("username") String username, @Param("year") Integer year);

    // Average Expense
    @Query("SELECT COALESCE(AVG(e.amount), 0) FROM Expense e WHERE e.user.username = :username")
    Double getAverageExpense(@Param("username") String username);

    // Yearly Expenses
    @Query("SELECT new com.app.expense_tracker.dto.analytics.YearlyExpenseDTO(YEAR(e.date), SUM(e.amount)) " +
            "FROM Expense e WHERE e.user.username = :username " +
            "GROUP BY YEAR(e.date) ORDER BY YEAR(e.date)")
    List<YearlyExpenseDTO> getYearlyTotals(@Param("username") String username);

    // =========================================================================
    // ADVANCED SEARCH & AUTOMATION CONTROLLERS
    // =========================================================================

    @Query("SELECT e FROM Expense e WHERE e.user.username = :username " +
            "AND (:category IS NULL OR :category = '' OR e.category = :category) " +
            "AND (:type IS NULL OR :type = '' OR UPPER(e.type) = UPPER(:type)) " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(e.title) LIKE LOWER(:keyword) OR LOWER(e.category) LIKE LOWER(:keyword))")
    Page<Expense> searchAdvanced(
            @Param("username") String username,
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("type") String type,
            Pageable pageable
    );

    @Query("SELECT e FROM Expense e WHERE e.user.username = :username ORDER BY e.id DESC")
    List<Expense> findTop5ByUserUsernameOrderByIdDesc(String username);

    List<Expense> findByUser(User user);
    List<Expense> findByUserAndDateBetween(User user, LocalDate start, LocalDate end);
    List<Expense> findByDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT e FROM Expense e WHERE e.user.username = :username AND e.recurring = true AND e.nextDate BETWEEN :startDate AND :endDate")
    List<Expense> findUpcomingRecurringExpenses(
            @Param("username") String username,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Transactional
    @Modifying
    @Query("DELETE FROM Expense e WHERE e.user.username = :username")
    void deleteByByUserUsername(@Param("username") String username);

    // Aggregates sums grouping by type (INCOME vs EXPENSE) for a specific user, year, and month
    @Query("SELECT e.type, SUM(e.amount) FROM Expense e " +
            "WHERE e.user.id = :userId AND YEAR(e.date) = :year AND MONTH(e.date) = :month " +
            "GROUP BY e.type")
    List<Object[]> getMonthlyCashFlow(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);


    // Pulls categories ordered by highest spending weight
    @Query("SELECT e.category, SUM(e.amount) FROM Expense e " +
            "WHERE e.user.id = :userId AND e.type = 'EXPENSE' " +
            "AND YEAR(e.date) = :year AND MONTH(e.date) = :month " +
            "GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> getMonthlyExpensesByCategory(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);


    // 🌟 Sums total amounts dynamically by Type, Year, and Month filters
    @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM Expense e WHERE " +
            "(:type IS NULL OR e.type = :type) AND " +
            "(:year IS NULL OR EXTRACT(YEAR FROM e.date) = :year) AND " +
            "(:month IS NULL OR EXTRACT(MONTH FROM e.date) = :month)")
    double sumAmountByFilters(@Param("type") String type,
                              @Param("year") Integer year,
                              @Param("month") Integer month);

    // 🌟 Counts transactions dynamically by Type, Year, and Month filters
    @Query("SELECT COUNT(e) FROM Expense e WHERE " +
            "(:type IS NULL OR e.type = :type) AND " +
            "(:year IS NULL OR EXTRACT(YEAR FROM e.date) = :year) AND " +
            "(:month IS NULL OR EXTRACT(MONTH FROM e.date) = :month)")
    long countByFilters(@Param("type") String type,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

    // 🌟 Finds most used category for true expenses matching timeframe parameters
    @Query(value = "SELECT e.category FROM expenses e WHERE " +
            "e.type = 'EXPENSE' AND " +
            "(:year IS NULL OR EXTRACT(YEAR FROM e.date) = :year) AND " +
            "(:month IS NULL OR EXTRACT(MONTH FROM e.date) = :month) " +
            "GROUP BY e.category ORDER BY COUNT(e.category) DESC LIMIT 1", nativeQuery = true)
    String findTopCategoryByFilters(@Param("year") Integer year,
                                    @Param("month") Integer month);
}