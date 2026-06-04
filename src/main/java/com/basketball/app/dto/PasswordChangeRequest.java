package com.basketball.app.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeRequest {
    private String currentPassword;

    @jakarta.validation.constraints.NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    @jakarta.validation.constraints.NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}

