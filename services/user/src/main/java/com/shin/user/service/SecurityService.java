package com.shin.user.service;

public interface SecurityService {

    String encryptPassword(String password);
    String decryptPassword(String passwordHash);

}
