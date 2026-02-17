package com.shin.auth.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.shin.auth.exception.InvalidTokenException;
import com.shin.auth.exception.RefreshTokenGenerationException;
import com.shin.auth.exception.TokenGenerationException;
import com.shin.auth.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
public class JwtTokenServiceImpl implements TokenService {

    @Value("${jwt.secret}")
    private String TOKEN_SECRET;

    private static final Long TOKEN_EXPIRES_SECONDS = 15L * 60L; // 15 minutes;

    @Override
    public String generateToken(String userId) {
        try{
            Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);

            return JWT.create()
                    .withIssuer("shin")
                    .withClaim("role", "USER")
                    .withSubject(userId)
                    .withExpiresAt(Instant.now().plusSeconds(TOKEN_EXPIRES_SECONDS))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new TokenGenerationException("Error while generating token " + exception.getMessage());
        }
    }

    @Override
    public String generateRefreshToken() {
        try {
            byte[] bytes = new  byte[32];

            SecureRandom.getInstanceStrong().nextBytes(bytes);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());

            throw new RefreshTokenGenerationException("Error while generating refresh token " + e.getMessage());
        }
    }

    @Override
    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
            return JWT.require(algorithm)
                    .withIssuer("shin")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception){
            throw new InvalidTokenException("Received a invalid jwt token " + exception.getMessage());
        }
    }

    @Override
    public Instant getExpirationDate(String token) {
        Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);

        return JWT.require(algorithm)
                .withIssuer("shin")
                .build()
                .verify(token)
                .getExpiresAtAsInstant();
    }

}
