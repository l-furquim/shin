package com.shin.auth.service;

import java.time.Instant;

public interface TokenService {

    String generateToken(String userId);
    String generateRefreshToken();
    String validateToken(String token);
    Instant getExpirationDate(String token);

}
