package com.shin.streaming.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.shin.streaming.config.CloudFrontConfig;
import com.shin.streaming.exception.InvalidTokenException;
import com.shin.streaming.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final CloudFrontConfig cloudFrontConfig;

    @Override
    public String generatePlaybackToken(UUID sessionId, UUID videoId, UUID userId) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        return JWT.create()
                .withIssuer("shin")
                .withSubject(userId.toString())
                .withClaim("sessionId", sessionId.toString())
                .withClaim("videoId", videoId.toString())
                .withExpiresAt(Date.from(Instant.now().plusSeconds(cloudFrontConfig.getCookieValiditySeconds())))
                .sign(algorithm);
    }


    @Override
    public DecodedJWT getToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            return JWT.require(algorithm)
                    .withIssuer("shin")
                    .build()
                    .verify(token);
        } catch (JWTVerificationException exception){
            throw new InvalidTokenException();
        }
    }

}
