package com.shin.auth.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRequest(

        @Email
        String email,

        @NotBlank
        String password,

        @Nullable
        String deviceId
) {
}
