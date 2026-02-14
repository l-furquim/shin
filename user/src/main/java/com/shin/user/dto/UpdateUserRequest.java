package com.shin.user.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateUserRequest(
        @org.hibernate.validator.constraints.UUID @NotNull UUID id,
        @Email @Nullable String email,
        @Nullable String displayName,
        @Nullable Boolean showAdultContent
) {
}
