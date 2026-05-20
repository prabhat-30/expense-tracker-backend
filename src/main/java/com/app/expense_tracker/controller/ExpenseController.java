package com.app.expense_tracker.controller;

import com.app.expense_tracker.dto.ExpenseDTO;
import com.app.expense_tracker.dto.analytics.DashboardSummaryDTO;
import com.app.expense_tracker.dto.analytics.YearlyExpenseDTO;
import com.app.expense_tracker.entity.Expense;
import com.app.expense_tracker.service.ExpenseService;
import com.app.expense_tracker.service.RecurringExpenseService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.app.expense_tracker.dto.analytics.CategoryTotalDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService service;

    @Autowired
    private RecurringExpenseService recurringExpenseService;

    @GetMapping("/upcoming-billings")
    public ResponseEntity<?> getUpcomingBillings(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Error: User session is unauthenticated.");
        }
        return ResponseEntity.ok(recurringExpenseService.getUpcomingBillings(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<Expense> addExpense(@Valid @RequestBody ExpenseDTO dto, Authentication authentication){
        Expense expense = service.addExpense(dto, authentication.getName());
        return new ResponseEntity<>(expense, HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Expense>> addAllExpenses(@Valid @RequestBody List<ExpenseDTO> dtos, Authentication authentication) {
        return new ResponseEntity<>(service.addAllExpenses(dtos, authentication.getName()), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/stop-recurring")
    public ResponseEntity<Expense> stopRecurring(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(service.stopRecurring(id, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<Page<Expense>> getAllExpenses(@Valid Pageable pageable, Authentication authentication){
        return ResponseEntity.ok(service.getAllExpense(authentication.getName(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpenseById(@Valid @PathVariable Long id, Authentication authentication){
        return ResponseEntity.ok(service.getExpenseById(id, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteExpense(@PathVariable Long id, Authentication authentication){
        service.deleteExpense(id, authentication.getName());
        return ResponseEntity.ok("Expense id: " + id + " deleted successfully");
    }

    @DeleteMapping
    public ResponseEntity<String> deleteAllExpenses(Authentication authentication){
        service.deleteAllExpenses(authentication.getName());
        return ResponseEntity.ok("Successfully Deleted All Records");
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(@PathVariable Long id, @Valid @RequestBody ExpenseDTO dto, Authentication authentication){
        return ResponseEntity.ok(service.updateExpense(id, dto, authentication.getName()));
    }

    @GetMapping("/category/{category}")
    public Page<Expense> getByCategory(@PathVariable String category, Pageable pageable, Authentication authentication){
        return service.getByCategory(authentication.getName(), category, pageable);
    }

    @GetMapping("/search/by-title")
    public Page<Expense> getByTitle(@RequestParam String title, Pageable pageable, Authentication authentication){
        return service.getByTitle(authentication.getName(), title, pageable);
    }

    @GetMapping("/amount/{amount}")
    public Page<Expense> getByAmount(@PathVariable Double amount, Pageable pageable, Authentication authentication){
        return service.getByAmount(authentication.getName(), amount, pageable);
    }

    @GetMapping("/title/{title}/category/{category}")
    public Page<Expense> getByTitleAndCategory(@PathVariable String title, @PathVariable String category, Pageable pageable, Authentication authentication){
        return service.getByTitleAndCategory(authentication.getName(), title, category, pageable);
    }

    @GetMapping("/search/by-amount-greater-than")
    public Page<Expense> getByAmountGreaterThan(@RequestParam Double amount, Pageable pageable, Authentication authentication){
        return service.getByAmountGreaterThan(authentication.getName(), amount, pageable);
    }

    @GetMapping("/date/{date}")
    public List<Expense> getByDateBetween(@PathVariable LocalDate date, Authentication authentication) {
        return service.getByDate(authentication.getName(), date);
    }

    @GetMapping("/date-between")
    public Page<Expense> getByDateBetween(@RequestParam LocalDate start, @RequestParam LocalDate end, Pageable pageable, Authentication authentication){
        return service.getByDateBetween(authentication.getName(), start, end, pageable);
    }

    @GetMapping("/analytics/total")
    public ResponseEntity<Double> getTotalExpense(Authentication authentication){
        return ResponseEntity.ok(service.getTotalExpense(authentication.getName()));
    }

    @GetMapping("/analytics/count")
    public ResponseEntity<Long> getExpenseCount(Authentication authentication){
        return ResponseEntity.ok(service.getExpenseCount(authentication.getName()));
    }

    @GetMapping("/analytics/highest")
    public ResponseEntity<Expense> getHighestExpense(Authentication authentication){
        // This unwraps the Optional. If it's empty, it safely assigns null instead of crashing.
        Expense highest = service.getHighestExpense(authentication.getName()).orElse(null);
        return ResponseEntity.ok(highest);
    }

    @GetMapping("/analytics/recent")
    public ResponseEntity<List<Expense>> getRecentTransactions(Authentication authentication){
        return ResponseEntity.ok(service.getRecentExpenses(authentication.getName()));
    }

    @GetMapping("/analytics/category-totals")
    public ResponseEntity<List<CategoryTotalDTO>> getCategoryTotals(Authentication authentication){
        return ResponseEntity.ok(service.getCategoryTotals(authentication.getName()));
    }

    @GetMapping("/analytics/monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyAnalytics(
            @RequestParam(value = "year", required = false, defaultValue = "ALL") String year, Authentication authentication) {
        return ResponseEntity.ok(service.getMonthlyTotals(authentication.getName(), year));
    }

    @GetMapping("/analytics/yearly")
    public ResponseEntity<List<YearlyExpenseDTO>> getYearlyTotals(Authentication authentication){
        return ResponseEntity.ok(service.getYearlyTotals(authentication.getName()));
    }

    @GetMapping("/analytics/average")
    public ResponseEntity<Double> getAverageExpense(Authentication authentication){
        return ResponseEntity.ok(service.getAverageExpense(authentication.getName()));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary(Authentication authentication) {
        return ResponseEntity.ok(service.getDashboardSummary(authentication.getName()));
    }

    // FIXED MAP ROUTE: Your client-side Dashboard & Profile expect the full unified DTO on this route!
    @GetMapping("/analytics/summary")
    public ResponseEntity<DashboardSummaryDTO> getOverviewAnalyticsSummary(
            @RequestParam(value = "year", required = false, defaultValue = "ALL") String year,
            @RequestParam(value = "month", required = false) Integer month,
            Authentication authentication) {

        // When filtering by alternative criteria, build a dynamic summary wrapper matching the contract parameters
        return ResponseEntity.ok(service.getDashboardSummaryByYearAndMonth(authentication.getName(), year, month));
    }

    @GetMapping("/search/advanced")
    public ResponseEntity<Page<Expense>> searchAdvanced(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            Pageable pageable, Authentication authentication) {
        return ResponseEntity.ok(service.searchAdvanced(authentication.getName(), category, keyword, type, pageable));
    }
}