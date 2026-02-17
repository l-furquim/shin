package com.shin.auth.controller;

import com.shin.auth.dto.AuthRequest;
import com.shin.auth.dto.AuthResponse;
import com.shin.auth.dto.LogoutResponse;
import com.shin.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping()
    public ResponseEntity<AuthResponse> auth(
        @RequestHeader("X-Client-IP") String clientIp,
        @RequestHeader("User-Agent") String agent,
        @Valid @RequestBody AuthRequest authRequest
    ) {
        var response = authService.auth(agent, clientIp, authRequest);

        ResponseCookie sessionCookie = ResponseCookie.from("refreshToken", response.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .body(new AuthResponse(
                response.token(),
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

            ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

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

        ResponseCookie sessionCookie = ResponseCookie.from("refreshToken", response.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .body(new AuthResponse(
                        response.token(),
                        response.tokenExpiresIn()));
    }

}
