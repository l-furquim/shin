package com.shin.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateCreatorRequest(
        @Size(max = 100, message = "Username must be less than 100 characters")
        String username,

        @Size(max = 500, message = "Description must be less than 500 characters")
        String description,

        // User-related fields
        @Size(max = 200, message = "Display name must be less than 200 characters")
        String displayName,

        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must be less than 100 characters")
        String email,

        Boolean showAdultContent
) {
}
