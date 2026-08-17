package com.nforceone.nforcehq.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Company name is required") String orgName,
        @NotBlank(message = "Your name is required") String adminFullName,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String adminEmail,
        @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password) {
}
