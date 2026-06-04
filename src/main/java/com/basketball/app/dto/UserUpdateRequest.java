package com.basketball.app.dto;

import com.basketball.app.model.User;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String name;

    private String surname;

    @Email(message = "Email must be valid")
    private String email;
    
    private User.Role role;
    private Long categoryId;
    private java.time.LocalDate dateOfBirth;
    private java.util.List<Long> categoryIds;
    private Boolean isActive;
}

