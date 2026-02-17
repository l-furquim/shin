package com.shin.auth.service.impl;

import com.shin.auth.client.UserClient;
import com.shin.auth.dto.AuthUserRequest;
import com.shin.auth.dto.AuthUserResponse;
import com.shin.auth.service.UserClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserClientServiceImpl implements UserClientService {

    private final UserClient userClient;

    @Override
    public AuthUserResponse getUserById(AuthUserRequest authUserRequest) {
        try {

            var response = userClient.auth(authUserRequest);

            var body = response.getBody();

            log.debug("REspota: {}", body);

            return body;
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            return null;
        }
    }
}
