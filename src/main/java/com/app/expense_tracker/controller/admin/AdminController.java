package com.app.expense_tracker.controller.admin;

import com.app.expense_tracker.dto.admin.RoleUpdateRequest;
import com.app.expense_tracker.entity.AuditLog;
import com.app.expense_tracker.entity.User;
import com.app.expense_tracker.service.admin.AdminService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // GET ALL USERS
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {

        return ResponseEntity.ok(
                adminService.getAllUsers()
        );
    }

    // DISABLE USER
    @PutMapping("/disable/{id}")
    public ResponseEntity<String> disableUser(
            @PathVariable Long id,
            Authentication authentication) {

        String adminUsername =
                authentication.getName();

        return ResponseEntity.ok(
                adminService.disableUser(
                        id,
                        adminUsername
                )
        );
    }

    // ENABLE USER
    @PutMapping("/enable/{id}")
    public ResponseEntity<String> enableUser(
            @PathVariable Long id,
            Authentication authentication) {

        String adminUsername =
                authentication.getName();

        return ResponseEntity.ok(
                adminService.enableUser(
                        id,
                        adminUsername
                )
        );
    }

    // CHANGE ROLE
    @PutMapping("/role/{id}")
    public ResponseEntity<String> changeRole(
            @PathVariable Long id,
            @RequestBody RoleUpdateRequest request,
            Authentication authentication) {

        String adminUsername =
                authentication.getName();

        return ResponseEntity.ok(
                adminService.changeRole(
                        id,
                        request.getRole(),
                        adminUsername
                )
        );
    }

    // DELETE USER
    @DeleteMapping("/user/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {

        String adminUsername =
                authentication.getName();

        return ResponseEntity.ok(
                adminService.deleteUser(
                        id,
                        adminUsername
                )
        );
    }

    // GET AUDIT LOGS
    // AuditLogController.java
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(Pageable pageable) {
        // This allows the frontend to send ?page=0&size=10&sort=id,desc
        return ResponseEntity.ok(adminService.findAll(pageable));
    }

}