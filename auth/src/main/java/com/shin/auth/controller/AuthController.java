package com.shin.auth.controller;

import com.shin.auth.dto.AuthRequest;
import com.shin.auth.dto.AuthResponse;
import com.shin.auth.dto.LogoutResponse;
import com.shin.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.version}/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${auth.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${auth.cookie.http-only:true}")
    private boolean cookieHttpOnly;

    @Value("${auth.cookie.path:/}")
    private String cookiePath;

    @Value("${auth.cookie.same-site:Strict}")
    private String cookieSameSite;

    @Value("${auth.cookie.max-age-days:7}")
    private long cookieMaxAgeDays;

    @PostMapping()
    public ResponseEntity<AuthResponse> auth(
        @RequestHeader("X-Client-IP") String clientIp,
        @RequestHeader("User-Agent") String agent,
        @Valid @RequestBody AuthRequest authRequest
    ) {
        var response = authService.auth(agent, clientIp, authRequest);

        ResponseCookie sessionCookie = buildRefreshCookie(response.refreshToken(), Duration.ofDays(cookieMaxAgeDays));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .body(new AuthResponse(
                response.token(),
                response.deviceId(),
                response.tokenExpiresIn()));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ){
        if(refreshToken == null || refreshToken.isBlank()){
            return ResponseEntity.badRequest()
                    .body(new LogoutResponse("Refresh token is required"));
        }

        var response = authService.logout(refreshToken);

            ResponseCookie clearCookie = buildRefreshCookie("", Duration.ZERO);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader("X-Client-IP") String clientIp,
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ){
        if(refreshToken == null || refreshToken.isBlank()){
            return ResponseEntity.status(401).build();
        }

        var response = authService.refresh(refreshToken, clientIp);

        ResponseCookie sessionCookie = buildRefreshCookie(response.refreshToken(), Duration.ofDays(cookieMaxAgeDays));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .body(new AuthResponse(
                        response.token(),
                        response.deviceId(),
                        response.tokenExpiresIn()));
    }

    private ResponseCookie buildRefreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path(cookiePath)
                .maxAge(maxAge)
                .sameSite(cookieSameSite)
                .build();
    }

}
