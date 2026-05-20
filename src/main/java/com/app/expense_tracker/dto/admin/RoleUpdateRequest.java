package com.app.expense_tracker.dto.admin;

import com.app.expense_tracker.entity.Role;

public class RoleUpdateRequest {
    private Role role;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
