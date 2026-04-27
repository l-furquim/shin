package com.shin.auth.model;

public record TokenClaim(
        String userId,
        String role,
        String displayName
) {
}
