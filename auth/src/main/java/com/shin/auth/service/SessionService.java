package com.shin.auth.service;

import com.shin.auth.model.Session;

import java.util.List;

public interface SessionService {

    String createSession(
            String agent,
            String address,
            String refreshToken,
            String accessToken,
            String userId,
            String deviceId
    );

    Session getSessionByRefreshToken(String refreshToken);
    
    Session getSessionByDeviceId(String deviceId);

    List<Session> getAllUserSessions(String userId);

    void revokeSession(String refreshToken);
    
    void revokeAllUserSessions(String userId);
    
    void refreshSession(String oldRefreshToken, String newRefreshToken, String newAccessToken, String address);


}
