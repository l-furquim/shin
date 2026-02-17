package com.shin.user.controller;

import com.shin.user.dto.*;
import com.shin.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${api.version}/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/auth")
    public ResponseEntity<AuthResponse> auth(
            @RequestBody AuthRequest authRequest
    ) {
        var response =  userService.auth(authRequest);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetUserByIdResponse> getUser(
        @PathVariable UUID id
    ){
         var response = userService.getUserById(id);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UpdateUserResponse> update(
        @PathVariable UUID id,
        @RequestHeader("X-User-Id") UUID userId,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        UpdateUserResponse response = userService.updateUser(id, userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
