package com.shin.streaming.service;

import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.UUID;

public interface AuthService {
    String generatePlaybackToken(UUID sessionId, UUID videoId, UUID userId);
    DecodedJWT getToken(String token);
}
