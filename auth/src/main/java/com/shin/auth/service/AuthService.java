package com.shin.auth.service;

import com.shin.auth.dto.AuthRequest;
import com.shin.auth.dto.LogoutResponse;
import com.shin.auth.dto.TokenResponse;

public interface AuthService {

    TokenResponse auth(String userAgent, String address, AuthRequest authRequest);
    LogoutResponse logout(String token);
    TokenResponse refresh(String refreshToken, String address);

}
