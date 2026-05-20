package com.app.expense_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Security validation context token parameter missing")
    private String token;

    @NotBlank(message = "New tracking credential password input required")
    private String newPassword;

    @NotBlank(message = "Confirmation entry missing")
    private String confirmPassword;
}