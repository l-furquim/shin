package com.shin.auth.service;

import com.shin.auth.dto.AuthUserRequest;
import com.shin.auth.dto.AuthUserResponse;

public interface UserClientService {

    AuthUserResponse getUserById(AuthUserRequest authUserRequest);

}
