package com.shin.metadata.controller;

import com.shin.metadata.dto.VideoLikesResponse;
import com.shin.metadata.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.version}/videos/{id}/likes")
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<VideoLikesResponse> like(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("id") UUID videoId
    ) {
        final var response = likeService.like(userId, videoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping
    public ResponseEntity<VideoLikesResponse> unlike(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("id") UUID videoId
    ) {
        final var response = likeService.unlike(userId, videoId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<VideoLikesResponse> getLikeInfo(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("id") UUID videoId
    ) {
        final var response = likeService.getLikeInfo(userId, videoId);
        return ResponseEntity.ok(response);
    }
}
