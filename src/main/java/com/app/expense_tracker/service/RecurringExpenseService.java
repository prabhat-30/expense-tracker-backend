package com.app.expense_tracker.service;

import com.app.expense_tracker.entity.Expense;
import com.app.expense_tracker.repository.ExpenseRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek; // Added for weekend check
import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;


    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processRecurringExpenses() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        List<Expense> templates = expenseRepository.findPendingRecurringExpenses(today);

        for (Expense template : templates) {
            boolean shouldAddTransaction = true;

            // Conditional Logic for Daily Commuters
            if ("DAILY".equalsIgnoreCase(template.getFrequency())) {
                if (dayOfWeek == DayOfWeek.SATURDAY && !template.isIncludeSat()) {
                    shouldAddTransaction = false;
                } else if (dayOfWeek == DayOfWeek.SUNDAY && !template.isIncludeSun()) {
                    shouldAddTransaction = false;
                }
            }

            if (shouldAddTransaction) {
                // 1. Create the new entry
                Expense newTransaction = new Expense();
                newTransaction.setAmount(template.getAmount());
                newTransaction.setCategory(template.getCategory());
                newTransaction.setTitle(template.getTitle());
                newTransaction.setType(template.getType());
                newTransaction.setDate(today);
                newTransaction.setUser(template.getUser());

                // Ensure the new transaction isn't marked as a template itself
                newTransaction.setRecurring(false);

                expenseRepository.save(newTransaction);
            }

            // 2. ALWAYS update the template's nextDate so the scheduler moves forward
            template.setNextDate(calculateNextDate(today, template.getFrequency()));
            expenseRepository.save(template);
        }
    }

    private LocalDate calculateNextDate(LocalDate current, String frequency) {
        return switch (frequency.toUpperCase()) {
            case "DAILY" -> current.plusDays(1);
            case "WEEKLY" -> current.plusWeeks(1);
            case "MONTHLY" -> current.plusMonths(1);
            case "YEARLY" -> current.plusYears(1);
            default -> current.plusDays(1);
        };
    }

    public List<Expense> getUpcomingBillings(String username) {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);

        return expenseRepository.findUpcomingRecurringExpenses(username, today, sevenDaysFromNow);
    }

}