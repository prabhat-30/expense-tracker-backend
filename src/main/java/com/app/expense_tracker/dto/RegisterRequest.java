package com.app.expense_tracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Name field is required")
    private String name;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email format description")
    private String email;

    @NotBlank(message = "Phone number tracking is required")
    private String phoneNo;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 30, message = "Password complexity must remain between 6 to 30 keys")
    private String password;

    @NotBlank(message = "Confirmation password is required")
    private String confirmPassword;
}