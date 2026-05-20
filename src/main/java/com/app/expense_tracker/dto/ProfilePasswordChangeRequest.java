package com.app.expense_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfilePasswordChangeRequest {
    @NotBlank(message = "Old authentication password key missing")
    private String oldPassword;

    @NotBlank(message = "Target transformation password constraint missing")
    private String newPassword;

    @NotBlank(message = "Confirmation parameter evaluation missing")
    private String confirmPassword;
}