package com.shin.auth.service.impl;

import com.shin.auth.dto.AuthRequest;
import com.shin.auth.dto.AuthUserRequest;
import com.shin.auth.dto.LogoutResponse;
import com.shin.auth.dto.TokenResponse;
import com.shin.auth.exception.InvalidCredentialsException;
import com.shin.auth.service.AuthService;
import com.shin.auth.service.SessionService;
import com.shin.auth.service.TokenService;
import com.shin.auth.service.UserClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final SessionService sessionService;
    private final TokenService tokenService;
    private final UserClientService userClientService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public TokenResponse auth(
            String userAgent,
            String address,
            AuthRequest authRequest
    ) {
        log.info("Received auth request from {}, with data {}", address, authRequest);

        var authResponse = userClientService.getUserById(new AuthUserRequest(
                authRequest.email()
        ));

        if(authResponse == null){
            throw new InvalidCredentialsException("Invalid credentials.");
        }

        boolean passwordMatch = bCryptPasswordEncoder.matches(authRequest.password(), authResponse.encryptedPassword());

        if(!passwordMatch){
            throw new InvalidCredentialsException("Invalid credentials.");
        }

        final var refreshToken = this.tokenService.generateRefreshToken();
        final var token = this.tokenService.generateToken(authResponse.id().toString());
        final var userId = authResponse.id().toString();

        if(authRequest.deviceId() != null){
            final var currentSession = this.sessionService.getSessionByDeviceId(authRequest.deviceId());

            if(currentSession != null && currentSession.userId().equals(userId)){
                this.sessionService.refreshSession(currentSession.refreshToken(), refreshToken, token, address);

                return new TokenResponse(
                        token,
                        refreshToken,
                        this.tokenService.getExpirationDate(token).getEpochSecond()
                );
            }
        }

        String deviceId = authRequest.deviceId() != null ? authRequest.deviceId() : UUID.randomUUID().toString();

        this.sessionService.createSession(
                userAgent,
                address,
                refreshToken,
                token,
                userId,
                deviceId
        );

        return new TokenResponse(
                token,
                refreshToken,
                this.tokenService.getExpirationDate(token).getEpochSecond()
        );
    }

    @Override
    public LogoutResponse logout(String refreshToken) {
        log.info("Logout requested for session");
        
        try {
            this.sessionService.revokeSession(refreshToken);
            return new LogoutResponse("Logged out successfully");
        } catch (Exception e) {
            log.error("Error during logout", e);
            throw new RuntimeException("Logout failed");
        }
    }

    @Override
    public TokenResponse refresh(String refreshToken, String address) {
        log.info("Token refresh requested from {}", address);
        
        try {
            var session = this.sessionService.getSessionByRefreshToken(refreshToken);
            
            if (session == null) {
                throw new InvalidCredentialsException("Invalid refresh token");
            }

            final var newRefreshToken = this.tokenService.generateRefreshToken();
            final var newAccessToken = this.tokenService.generateToken(session.userId());

            this.sessionService.refreshSession(refreshToken, newRefreshToken, newAccessToken, address);

            return new TokenResponse(
                    newAccessToken,
                    newRefreshToken,
                    this.tokenService.getExpirationDate(newAccessToken).getEpochSecond()
            );
        } catch (Exception e) {
            log.error("Error during token refresh", e);
            throw new RuntimeException("Token refresh failed");
        }
    }


}
