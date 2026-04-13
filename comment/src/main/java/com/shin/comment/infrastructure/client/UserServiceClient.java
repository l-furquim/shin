package com.shin.comment.infrastructure.client;

import com.shin.comment.infrastructure.dto.CreatorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/v1/creators/{id}")
    CreatorResponse getCreatorById(@PathVariable("id") UUID id);

}
