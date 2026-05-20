package com.app.expense_tracker.controller;

import com.app.expense_tracker.dto.YearlyReportDTO;
import com.app.expense_tracker.entity.Expense;
import com.app.expense_tracker.entity.User;
import com.app.expense_tracker.repository.ExpenseRepository;
import com.app.expense_tracker.repository.UserRepository;
import com.app.expense_tracker.service.ReportService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;

    public ReportController(
            ReportService reportService,
            UserRepository userRepository,
            ExpenseRepository expenseRepository
    ) {
        this.reportService = reportService;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
    }

    // ========================== PDF REPORT ==========================

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(Authentication authentication) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();

        byte[] pdf = reportService.generatePdf(user);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=expenses.pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ========================== EXCEL REPORT ==========================

    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(Authentication authentication) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();

        byte[] excel = reportService.generateExcel(user);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=expenses.xlsx"
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    // ========================== CSV REPORT ==========================

    @GetMapping("/csv")
    public ResponseEntity<byte[]> csv(Authentication authentication) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();

        byte[] csv = reportService.generateCsv(user);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=expenses.csv"
                )
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }

    // ========================== MONTHLY REPORT ==========================

    @GetMapping("/monthly")
    public ResponseEntity<List<Expense>> monthly(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication
    ) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();

        return ResponseEntity.ok(
                reportService.monthlyReport(
                        user,
                        year,
                        month
                )
        );
    }

    // ========================== YEARLY REPORT ==========================

    @GetMapping("/yearly/{year}")
    public ResponseEntity<YearlyReportDTO> yearlyReport(
            @AuthenticationPrincipal User user,
            @PathVariable int year
    ) {

        return ResponseEntity.ok(
                reportService.getYearlyReport(
                        user,
                        year
                )
        );
    }

    // ========================== HIGHEST SPENDING MONTH ==========================

    @GetMapping("/highest-month/{year}")
    public ResponseEntity<String> highestMonth(
            @AuthenticationPrincipal User user,
            @PathVariable int year
    ) {

        YearlyReportDTO report =
                reportService.getYearlyReport(user, year);

        String highestMonth =
                reportService.getHighestSpendingMonth(
                        report.getMonthlySummary()
                );

        return ResponseEntity.ok(highestMonth);
    }

    // ========================== SAVINGS ANALYSIS ==========================

    @GetMapping("/savings")
    public ResponseEntity<Double> savings(
            @RequestParam double income,
            @RequestParam double expense
    ) {

        return ResponseEntity.ok(
                reportService.calculateSavings(
                        income,
                        expense
                )
        );
    }

    // ========================== YEAR OVER YEAR GROWTH ==========================

    @GetMapping("/growth")
    public ResponseEntity<Double> growth(
            @RequestParam double currentYearExpense,
            @RequestParam double previousYearExpense
    ) {

        return ResponseEntity.ok(
                reportService.calculateGrowth(
                        currentYearExpense,
                        previousYearExpense
                )
        );
    }

    // ========================== MOST USED CATEGORY ==========================

    @GetMapping("/most-used-category")
    public ResponseEntity<String> mostUsedCategory(
            @AuthenticationPrincipal User user
    ) {

        List<Expense> expenses =
                expenseRepository.findByUser(user);

        return ResponseEntity.ok(
                reportService.getMostUsedCategory(
                        expenses
                )
        );
    }

    // ========================== MONTHLY SUMMARY ==========================

    @GetMapping("/monthly-summary/{year}")
    public ResponseEntity<Map<String, Double>> monthlySummary(
            @AuthenticationPrincipal User user,
            @PathVariable int year
    ) {

        YearlyReportDTO report =
                reportService.getYearlyReport(user, year);

        return ResponseEntity.ok(
                report.getMonthlySummary()
        );
    }

    // ========================== CATEGORY SUMMARY ==========================

    @GetMapping("/category-summary/{year}")
    public ResponseEntity<Map<String, Double>> categorySummary(
            @AuthenticationPrincipal User user,
            @PathVariable int year
    ) {

        YearlyReportDTO report =
                reportService.getYearlyReport(user, year);

        return ResponseEntity.ok(
                report.getCategorySummary()
        );
    }
}