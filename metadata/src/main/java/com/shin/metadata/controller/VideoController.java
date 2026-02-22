package com.shin.metadata.controller;

import com.shin.metadata.dto.*;
import com.shin.metadata.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${api.version}/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/init")
    public ResponseEntity<InitVideoResponse> initVideo(
            @RequestHeader("X-User-Id") String userId
    ) {
        var response = videoService.initVideo(userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping
    public ResponseEntity<CreateVideoResponse> createVideo(
        @Valid @RequestBody CreateVideoRequest request
    ) {
        CreateVideoResponse response = videoService.createVideo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetVideoByIdResponse> getVideoById(
        @PathVariable("id") UUID id,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        GetVideoByIdResponse response = videoService.getVideoById(id, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PatchVideoByIdResponse> patchVideoById(
        @PathVariable("id") UUID id,
        @Valid @RequestBody PatchVideoByIdRequest request
    ) {
        PatchVideoByIdResponse response = videoService.patchVideoById(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideoById(
        @PathVariable("id") UUID id
    ) {
        videoService.deleteVideoById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<SearchVideosResponse> search(
        @RequestParam(name = "accountId", required = false) String accountId,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "start", defaultValue = "0") int start,
        @RequestParam(name = "end", defaultValue = "10") int end
    ) {
        SearchVideosResponse response;

        if (accountId != null) {
            response = videoService.searchByAccountId(accountId, start, end);
        } else if (category != null) {
            response = videoService.searchByCategory(category, start, end);
        } else {
            response = videoService.searchAll(start, end);
        }

        if (response.videos().isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }
}
