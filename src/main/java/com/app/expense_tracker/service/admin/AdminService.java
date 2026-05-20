package com.app.expense_tracker.service.admin;

import com.app.expense_tracker.entity.AuditLog;
import com.app.expense_tracker.entity.Role;
import com.app.expense_tracker.entity.User;
import com.app.expense_tracker.repository.AuditLogRepository;
import com.app.expense_tracker.repository.UserRepository;
import com.app.expense_tracker.service.AuditLogService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;

    public AdminService(UserRepository userRepository,
                        AuditLogService auditLogService,
                        AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.auditLogRepository = auditLogRepository;
    }

    // GET ALL USERS
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // DISABLE USER
    public String disableUser(Long id, String adminUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 👑 HARD GUARDRAIL: Rejects attempts to block the primary seed administrator
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new RuntimeException("Security Exception: The Primary Admin account cannot be disabled.");
        }

        // PREVENT SELF DISABLE
        if (user.getUsername().equals(adminUsername)) {
            throw new RuntimeException("Admin cannot disable own account");
        }

        if (!user.isActive()) {
            throw new RuntimeException("User already disabled");
        }

        user.setActive(false);
        userRepository.save(user);

        auditLogService.log(
                adminUsername,
                "USER_DISABLED",
                "Disabled user " + user.getUsername()
        );

        return "User disabled successfully";
    }

    // ENABLE USER
    public String enableUser(Long id, String adminUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isActive()) {
            throw new RuntimeException("User already enabled");
        }
        user.setActive(true);
        userRepository.save(user);

        auditLogService.log(
                adminUsername,
                "USER_ENABLED",
                "Enabled user " + user.getUsername()
        );

        return "User enabled successfully";
    }

    // CHANGE ROLE
    public String changeRole(Long id, Role role, String adminUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 👑 HARD GUARDRAIL: Rejects attempts to downgrade the primary seed administrator
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new RuntimeException("Security Exception: Primary Admin privileges cannot be altered.");
        }

        // PREVENT SELF ROLE CHANGE
        if (user.getUsername().equals(adminUsername)) {
            throw new RuntimeException("Admin cannot change own role");
        }

        if (user.getRole() == role) {
            throw new RuntimeException("User already has this role");
        }
        user.setRole(role);
        userRepository.save(user);

        auditLogService.log(
                adminUsername,
                "ROLE_CHANGED",
                "Changed role of user " + user.getUsername() + " to " + role.name()
        );

        return "Role updated successfully";
    }

    // DELETE USER
    public String deleteUser(Long id, String adminUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 👑 HARD GUARDRAIL: Rejects attempts to delete the primary seed administrator
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new RuntimeException("Security Exception: The Primary Admin account cannot be deleted.");
        }

        // PREVENT SELF DELETE
        if (user.getUsername().equals(adminUsername)) {
            throw new RuntimeException("Admin cannot delete own account");
        }

        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Admin user cannot be delete");
        }
        userRepository.delete(user);

        auditLogService.log(
                adminUsername,
                "DELETE_USER",
                "Deleted user " + user.getUsername()
        );

        return "User deleted successfully";
    }

    // GET AUDIT LOGS
    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}