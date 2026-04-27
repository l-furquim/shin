package com.shin.user.service;

import com.shin.user.dto.*;

import java.util.UUID;

public interface UserService {

    AuthResponse auth(AuthRequest authRequest);

    UpdateUserResponse updateUser(UUID id, UUID requesterId, UpdateUserRequest updateUserRequest);

    GetUserByIdResponse getUserById(UUID id);

}
