package com.shin.auth.client;

import com.shin.auth.dto.AuthUserRequest;
import com.shin.auth.dto.AuthUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "user-service")
public interface UserClient {

    @PostMapping("/api/v1/users/auth")
    ResponseEntity<AuthUserResponse> auth(
            @RequestBody AuthUserRequest authRequest
    );

}
