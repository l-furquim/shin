package com.shin.auth.service.impl;

import com.shin.auth.exception.SessionExpiredException;
import com.shin.auth.exception.SessionRetrievalException;
import com.shin.auth.model.Session;
import com.shin.auth.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class SessionServiceImpl implements SessionService {

    private final RedisTemplate<String, Session> redisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;

    private static final Long REFRESH_TOKEN_EXPIRES_SECONDS = 7L * 24L * 60L * 60L; // 1 week
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    private static final String DEVICE_TO_TOKEN_PREFIX = "device_token:";


    @Override
    public String createSession(
            String agent,
            String address,
            String refreshToken,
            String accessToken,
            String userId,
            String deviceId
    ) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiration = now.plusSeconds(REFRESH_TOKEN_EXPIRES_SECONDS);

            Session session = new Session(
                    userId,
                    deviceId,
                    address,
                    refreshToken,
                    accessToken,
                    false,
                    agent,
                    expiration,
                    now,
                    now
            );

            redisTemplate.opsForValue().set(
                    SESSION_PREFIX + refreshToken,
                    session,
                    Duration.ofSeconds(REFRESH_TOKEN_EXPIRES_SECONDS)
            );

            stringRedisTemplate.opsForSet().add(USER_SESSIONS_PREFIX + userId, refreshToken);
            stringRedisTemplate.expire(USER_SESSIONS_PREFIX + userId, Duration.ofSeconds(REFRESH_TOKEN_EXPIRES_SECONDS));

            stringRedisTemplate.opsForValue().set(
                    DEVICE_TO_TOKEN_PREFIX + deviceId,
                    refreshToken,
                    Duration.ofSeconds(REFRESH_TOKEN_EXPIRES_SECONDS)
            );

            log.info("Session created for user: {}, deviceId: {}, IP: {}", userId, deviceId, address);
            
            return deviceId;
        } catch (Exception e) {
            log.error("Failed to create session for user: {}", userId, e);
            throw new SessionRetrievalException("Failed to create session");
        }
    }


    @Override
    public Session getSessionByRefreshToken(String refreshToken) {
        try {
            Session session = redisTemplate.opsForValue().get(SESSION_PREFIX + refreshToken);

            if (session == null) {
                log.debug("Session not found for refresh token");
                return null;
            }

            if (session.expiresAt().isBefore(LocalDateTime.now())) {
                log.info("Session expired for user: {}", session.userId());
                revokeSession(refreshToken);
                throw new SessionExpiredException();
            }

            if (session.revoked()) {
                log.warn("Attempt to use revoked session for user: {}", session.userId());
                throw new SessionExpiredException();
            }

            return session;
        } catch (SessionExpiredException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving session by refresh token: {}", e.getMessage());
            throw new SessionRetrievalException("Error retrieving session");
        }
    }


    @Override
    public Session getSessionByDeviceId(String deviceId) {
        try {
            String refreshToken = stringRedisTemplate.opsForValue().get(DEVICE_TO_TOKEN_PREFIX + deviceId);

            if (refreshToken == null) {
                log.debug("No session found for device: {}", deviceId);
                return null;
            }

            return getSessionByRefreshToken(refreshToken);
        } catch (SessionExpiredException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving session by device ID: {}", e.getMessage());
            throw new SessionRetrievalException("Error retrieving session");
        }
    }


    @Override
    public List<Session> getAllUserSessions(String userId) {
        try {
            Set<String> refreshTokens = stringRedisTemplate.opsForSet().members(USER_SESSIONS_PREFIX + userId);

            if (refreshTokens == null || refreshTokens.isEmpty()) {
                return List.of();
            }

            return refreshTokens.stream()
                    .map(token -> {
                        try {
                            return redisTemplate.opsForValue().get(SESSION_PREFIX + token);
                        } catch (Exception e) {
                            log.warn("Failed to retrieve session for token in user session list", e);
                            return null;
                        }
                    })
                    .filter(session -> session != null && !session.revoked() && session.expiresAt().isAfter(LocalDateTime.now()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving all user sessions for user: {}", userId, e);
            return List.of();
        }
    }


    @Override
    public void revokeSession(String refreshToken) {
        try {
            Session session = redisTemplate.opsForValue().get(SESSION_PREFIX + refreshToken);

            if (session == null) {
                log.debug("Session not found for revocation");
                return;
            }

            redisTemplate.delete(SESSION_PREFIX + refreshToken);

            stringRedisTemplate.opsForSet().remove(USER_SESSIONS_PREFIX + session.userId(), refreshToken);

            stringRedisTemplate.delete(DEVICE_TO_TOKEN_PREFIX + session.deviceId());

            log.info("Session revoked for user: {}, deviceId: {}", session.userId(), session.deviceId());
        } catch (Exception e) {
            log.error("Error revoking session: {}", e.getMessage(), e);
        }
    }


    @Override
    public void revokeAllUserSessions(String userId) {
        try {
            Set<String> refreshTokens = stringRedisTemplate.opsForSet().members(USER_SESSIONS_PREFIX + userId);

            if (refreshTokens == null || refreshTokens.isEmpty()) {
                log.debug("No sessions to revoke for user: {}", userId);
                return;
            }

            for (String refreshToken : refreshTokens) {
                Session session = redisTemplate.opsForValue().get(SESSION_PREFIX + refreshToken);
                
                if (session != null) {
                    redisTemplate.delete(SESSION_PREFIX + refreshToken);
                    
                    stringRedisTemplate.delete(DEVICE_TO_TOKEN_PREFIX + session.deviceId());
                }
            }

            stringRedisTemplate.delete(USER_SESSIONS_PREFIX + userId);

            log.info("All sessions revoked for user: {}, count: {}", userId, refreshTokens.size());
        } catch (Exception e) {
            log.error("Error revoking all user sessions for user: {}", userId, e);
        }
    }


    @Override
    public void refreshSession(String oldRefreshToken, String newRefreshToken, String newAccessToken, String address) {
        try {
            Session oldSession = redisTemplate.opsForValue().get(SESSION_PREFIX + oldRefreshToken);

            if (oldSession == null) {
                log.warn("Attempt to refresh non-existent session");
                throw new SessionRetrievalException("Session not found");
            }

            if (oldSession.revoked()) {
                log.warn("Attempt to refresh revoked session for user: {}", oldSession.userId());
                throw new SessionExpiredException();
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime newExpiration = now.plusSeconds(REFRESH_TOKEN_EXPIRES_SECONDS);

            Session refreshedSession = new Session(
                    oldSession.userId(),
                    oldSession.deviceId(),
                    address,
                    newRefreshToken,
                    newAccessToken,
                    false,
                    oldSession.userAgent(),
                    newExpiration,
                    now,
                    oldSession.createdAt()
            );

            redisTemplate.delete(SESSION_PREFIX + oldRefreshToken);
            stringRedisTemplate.opsForSet().remove(USER_SESSIONS_PREFIX + oldSession.userId(), oldRefreshToken);

            redisTemplate.opsForValue().set(
                    SESSION_PREFIX + newRefreshToken,
                    refreshedSession,
                    Duration.ofSeconds(REFRESH_TOKEN_EXPIRES_SECONDS)
            );

            stringRedisTemplate.opsForSet().add(USER_SESSIONS_PREFIX + oldSession.userId(), newRefreshToken);
            stringRedisTemplate.expire(USER_SESSIONS_PREFIX + oldSession.userId(), Duration.ofSeconds(REFRESH_TOKEN_EXPIRES_SECONDS));

            stringRedisTemplate.opsForValue().set(
                    DEVICE_TO_TOKEN_PREFIX + oldSession.deviceId(),
                    newRefreshToken,
                    Duration.ofSeconds(REFRESH_TOKEN_EXPIRES_SECONDS)
            );

            log.info("Session refreshed for user: {}, deviceId: {}", oldSession.userId(), oldSession.deviceId());
        } catch (SessionExpiredException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error refreshing session: {}", e.getMessage(), e);
            throw new SessionRetrievalException("Error refreshing session");
        }
    }
}
