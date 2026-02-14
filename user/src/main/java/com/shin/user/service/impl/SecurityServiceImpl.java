package com.shin.user.service.impl;

import com.shin.user.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SecurityServiceImpl implements SecurityService {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public String encryptPassword(String password) {
        return this.bCryptPasswordEncoder.encode(password);
    }

    @Override
    public String decryptPassword(String passwordHash) {
        return this.bCryptPasswordEncoder.matches(passwordHash, passwordHash) ? passwordHash : null;
    }
}
