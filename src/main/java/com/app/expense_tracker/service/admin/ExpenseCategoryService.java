package com.app.expense_tracker.service.admin;

import com.app.expense_tracker.entity.ExpenseCategory;
import com.app.expense_tracker.repository.ExpenseCategoryRepository;
import com.app.expense_tracker.security.SecuritySanitizer;
import com.app.expense_tracker.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @Autowired
    private SecuritySanitizer sanitizer;

    public ExpenseCategoryService(ExpenseCategoryRepository categoryRepository, AuditLogService auditLogService) {
        this.categoryRepository = categoryRepository;
        this.auditLogService = auditLogService;
    }

    // 🌟 Used by Admins to see all categories (including deactivated ones)
    public List<ExpenseCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    // 🌟 Used by Users to populate their dropdown entry forms with active options only
    public List<ExpenseCategory> getActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    // Create a new dynamic category platform-wide
    public ExpenseCategory createCategory(ExpenseCategory category, String adminUsername) {

        String safeName = sanitizer.sanitizeText(category.getName());
        String safeIcon = sanitizer.sanitizeText(category.getIcon());

        category.setName(safeName);
        category.setIcon(safeIcon);

        categoryRepository.findByNameIgnoreCase(category.getName())
                .ifPresent(c -> {
                    throw new RuntimeException("A category with this exact name already exists.");
                });

        ExpenseCategory saved = categoryRepository.save(category);

        // Audit Log high-impact infrastructure alteration
        auditLogService.log(
                adminUsername,
                "CATEGORY_CREATED",
                "Created new expense category: " + saved.getName() + " (" + saved.getIcon() + ")"
        );

        return saved;
    }

    // Toggle a category's visibility status (Active/Inactive)
    public ExpenseCategory toggleCategoryStatus(Long id, String adminUsername) {
        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Target category not found."));

        category.setActive(!category.isActive());
        ExpenseCategory updated = categoryRepository.save(category);

        // Audit Log high-impact toggle state change
        auditLogService.log(
                adminUsername,
                "CATEGORY_STATUS_TOGGLED",
                "Changed status of category [" + updated.getName() + "] to active=" + updated.isActive()
        );

        return updated;
    }
}