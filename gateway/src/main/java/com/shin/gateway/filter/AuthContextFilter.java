package com.shin.gateway.filter;

import com.shin.gateway.dto.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuthContextFilter implements GatewayFilter, Ordered {

    private final ReactiveRedisTemplate<String, Session> reactiveRedisTemplate;

    private static final String SESSION_PREFIX = "session:";
    private static final Duration SESSION_REFRESH_THRESHOLD = Duration.ofMinutes(30); // Refresh if less than 30min remaining

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return handleUnauthorized(exchange, "Authorization required.");
        }

        String authHeader = authHeaders.get(0);

        if (!authHeader.startsWith("Bearer ")) {
            return handleUnauthorized(exchange, "Invalid authorization header format");
        }

        String accessToken = authHeader.substring(7);
        if (accessToken.isBlank()) {
            return handleUnauthorized(exchange, "Access token required");
        }

        HttpCookie refreshTokenCookie = request.getCookies().getFirst("refreshToken");
        if (refreshTokenCookie == null) {
            return handleUnauthorized(exchange, "Refresh token cookie required");
        }

        String refreshToken = refreshTokenCookie.getValue();

        return validateSession(refreshToken)
                .flatMap(session -> {
                    if (!session.accessToken().equals(accessToken)) {
                        log.warn("Access token mismatch for user: {}", session.userId());
                        return handleUnauthorized(exchange, "Invalid access token");
                    }

                    LocalDateTime now = LocalDateTime.now();
                    Duration timeUntilExpiry = Duration.between(now, session.expiresAt());
                    
                    if (timeUntilExpiry.compareTo(SESSION_REFRESH_THRESHOLD) < 0) {
                        log.info("Session for user {} needs refresh. Time until expiry: {} minutes", 
                                session.userId(), timeUntilExpiry.toMinutes());
                    }

                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", session.userId())
                            .header("X-Device-Id", session.deviceId())
                            .header("X-Session-IP", session.ip())
                            .header("X-Gateway-Timestamp", String.valueOf(System.currentTimeMillis()))
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(e -> {
                    log.error("Error during session validation", e);
                    return handleUnauthorized(exchange, "Session validation failed");
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }


    private Mono<Session> validateSession(String refreshToken) {
        return reactiveRedisTemplate.opsForValue()
                .get(SESSION_PREFIX + refreshToken)
                .switchIfEmpty(Mono.error(new RuntimeException("Session not found")))
                .flatMap(session -> {

                    if (session.revoked()) {
                        log.warn("Attempt to use revoked session for user: {}", session.userId());
                        return Mono.error(new RuntimeException("Session revoked"));
                    }

                    LocalDateTime now = LocalDateTime.now();
                    if (session.expiresAt().isBefore(now)) {
                        log.warn("Session expired for user: {}. Expired at: {}, Current time: {}", 
                                session.userId(), session.expiresAt(), now);
                        return Mono.error(new RuntimeException("Session expired"));
                    }

                    return Mono.just(session);
                });
    }


    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        log.debug("Unauthorized request: {}", message);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format(
                "{\"error\":\"Unauthorized\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message, 
                Instant.now().toString()
        );
        
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
