package com.shin.user.service.impl;

import com.shin.user.dto.*;
import com.shin.user.exceptions.DuplicatedEmailException;
import com.shin.user.exceptions.UnauthorizedOperationException;
import com.shin.user.exceptions.UserNotFoundException;
import com.shin.user.model.User;
import com.shin.user.repository.UserRepository;
import com.shin.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public AuthResponse auth(AuthRequest authRequest) {
        log.info("Auth attempt: {}", authRequest);

        var user = this.findUserByEmail(authRequest.email());

        if(user == null) {
            throw new UserNotFoundException("Invalid credentials.");
        }
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getPassword()
        );
    }

    @Override
    public UpdateUserResponse updateUser(UUID id, UUID requesterId, UpdateUserRequest updateUserRequest) {
        if(!id.equals(requesterId)) {
            log.info("User with id {} attempted to update user with id {}", requesterId, id);
            throw new UnauthorizedOperationException("Unauthorized operation");
        }

        var user = findUserByIdOrThrow(id);

        if(updateUserRequest.displayName() != null && !updateUserRequest.displayName().isBlank()) {
            user.setDisplayName(updateUserRequest.displayName().trim());
        }

        if(updateUserRequest.email() != null && !updateUserRequest.email().isBlank()) {
            boolean emailAlreadyInUse = findUserByEmail(updateUserRequest.email()) != null
                    && !updateUserRequest.email().equals(user.getEmail());

            if(emailAlreadyInUse) {
                throw new DuplicatedEmailException("A user already exists with this email.");
            }

            user.setEmail(updateUserRequest.email().toLowerCase().trim());
        }

        if(updateUserRequest.showAdultContent() != null) {
            user.setShowAdultContent(updateUserRequest.showAdultContent());
        }

        var userUpdated = userRepository.save(user);

        log.info("User updated : {}", userUpdated.getId());

        return new UpdateUserResponse(
                userUpdated.getId(),
                userUpdated.getDisplayName(),
                userUpdated.getEmail(),
                userUpdated.getShowAdultContent(),
                userUpdated.getLanguageTag(),
                userUpdated.getUpdatedAt(),
                userUpdated.getCreatedAt()
        );
    }

    @Override
    public GetUserByIdResponse getUserById(UUID id) {
        var user = findUserByIdOrThrow(id);

        return new GetUserByIdResponse(
            user.getId(),
            user.getDisplayName(),
            user.getEmail(),
            user.getShowAdultContent(),
            user.getLanguageTag(),
            user.getUpdatedAt(),
            user.getCreatedAt()
        );
    }


    private User findUserByIdOrThrow(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found with id"));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElse(null);
    }

}
