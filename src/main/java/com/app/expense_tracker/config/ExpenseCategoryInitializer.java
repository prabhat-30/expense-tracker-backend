package com.app.expense_tracker.config;

import com.app.expense_tracker.entity.ExpenseCategory;
import com.app.expense_tracker.repository.ExpenseCategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ExpenseCategoryInitializer implements CommandLineRunner {

    private final ExpenseCategoryRepository categoryRepository;

    public ExpenseCategoryInitializer(ExpenseCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        // Seed default foundational categories if table stands empty
        initializeCategory("Food & Dining", "🍔");
        initializeCategory("Transport & Fuel", "🚗");
        initializeCategory("Rent & Housing", "🏠");
        initializeCategory("Utilities & Bills", "🔌");
        initializeCategory("Shopping & Lifestyle", "🛒");
        initializeCategory("Medical & Health", "🏥");
    }

    private void initializeCategory(String name, String icon) {
        if (categoryRepository.findByNameIgnoreCase(name).isEmpty()) {
            ExpenseCategory defaultCategory = new ExpenseCategory(name, icon, true);
            categoryRepository.save(defaultCategory);
            System.out.println("EXPENSE CATEGORY SEEDED: [" + icon + " " + name + "]");
        }
    }
}