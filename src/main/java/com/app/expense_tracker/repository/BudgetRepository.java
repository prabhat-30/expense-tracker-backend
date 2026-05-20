package com.app.expense_tracker.repository;

import com.app.expense_tracker.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserUsername(String username);
    Optional<Budget> findByUserUsernameAndCategory(String username, String category);
}