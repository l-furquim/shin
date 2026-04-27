package com.shin.user.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @Email String email,
    @Size(min = 8, max = 255, message = "Password must have at least 8 characters.") @NotNull String password,
    @NotBlank String displayName,
    @Nullable Boolean showAdultContent
) {
}
