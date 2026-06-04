package com.basketball.app.dto;

import com.basketball.app.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserCreateRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Surname is required")
    private String surname;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Role is required")
    private User.Role role;

    private Long categoryId;
    private java.time.LocalDate dateOfBirth; 
    private java.util.List<Long> categoryIds;
    private Boolean isActive = true;
    private Boolean mustChangePassword = true;
    
    private String password;
}

