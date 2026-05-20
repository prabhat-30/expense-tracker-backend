package com.app.expense_tracker.controller;

import com.app.expense_tracker.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<String> saveBudget(@RequestBody Map<String, Object> payload, Principal principal) {
        String category = (String) payload.get("category");
        Double limit = Double.parseDouble(payload.get("limitAmount").toString());

        expenseService.setBudget(principal.getName(), category, limit);
        return ResponseEntity.ok("Budget updated successfully");
    }
}