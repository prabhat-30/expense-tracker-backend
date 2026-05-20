package com.app.expense_tracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SocialLoginRequest {
    @NotBlank(message = "Name missing")
    private String name;

    @NotBlank(message = "Email missing")
    @Email
    private String email;

    @NotBlank(message = "OAuth platform identity tracking target required")
    private String provider; // "GOOGLE" or "APPLE"

    @NotBlank(message = "OAuth tracking unique payload serial identification string required")
    private String providerId;
}