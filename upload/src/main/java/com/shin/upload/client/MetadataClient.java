package com.shin.upload.client;

import com.shin.upload.dto.CreateVideoRequest;
import com.shin.upload.dto.UpdateVideoRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "metadata-service")
public interface MetadataClient {

    @PostMapping("/api/v1/videos")
    ResponseEntity<Object> createVideo(@RequestBody CreateVideoRequest request);

    @PatchMapping("/api/v1/videos/{id}")
    ResponseEntity<Object> updateVideo(
        @PathVariable UUID id,
        @RequestBody UpdateVideoRequest request
    );
}
