package com.shin.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCreatorRequest(
        @NotBlank(message = "Display name is required")
        @Size(max = 200, message = "Display name must be less than 200 characters")
        String displayName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must be less than 100 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        Boolean showAdultContent,

        @Size(max = 100, message = "Username must be less than 100 characters")
        String username,

        @Size(max = 500, message = "Description must be less than 500 characters")
        String description
) {
}

