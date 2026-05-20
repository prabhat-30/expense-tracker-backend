package com.app.expense_tracker.controller.admin;

import com.app.expense_tracker.entity.ExpenseCategory;
import com.app.expense_tracker.service.admin.ExpenseCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class ExpenseCategoryController {

    private final ExpenseCategoryService categoryService;

    public ExpenseCategoryController(ExpenseCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // 🌟 ENDPOINT FOR USERS: Fetch only visible active category items
    @GetMapping("/active")
    public ResponseEntity<List<ExpenseCategory>> getActiveCategories() {
        return ResponseEntity.ok(categoryService.getActiveCategories());
    }

    // 🌟 ENDPOINT FOR ADMINS: Fetch all platform tracking options
    @GetMapping("/admin/all")
    public ResponseEntity<List<ExpenseCategory>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    // 🌟 ENDPOINT FOR ADMINS: Create a fresh operational category tag
    @PostMapping("/admin/create")
    public ResponseEntity<ExpenseCategory> createCategory(
            @RequestBody ExpenseCategory category,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        return ResponseEntity.ok(categoryService.createCategory(category, adminUsername));
    }

    // 🌟 ENDPOINT FOR ADMINS: Deactivate/Reactivate an entry key row
    @PutMapping("/admin/toggle/{id}")
    public ResponseEntity<ExpenseCategory> toggleCategory(
            @PathVariable Long id,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        return ResponseEntity.ok(categoryService.toggleCategoryStatus(id, adminUsername));
    }
}