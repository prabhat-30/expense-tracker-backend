package com.app.expense_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Authentication login identity input parameter is required")
    private String identity; // Username, email, or phone number

    private String password; // Nullable if logging in strictly via OTP matching
    private String otp;      // Nullable if logging in strictly via password routing
}