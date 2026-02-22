package com.shin.interaction.controller;

import com.shin.interaction.dto.DeleteLikeResponse;
import com.shin.interaction.dto.GetVideoLikesResponse;
import com.shin.interaction.service.LikeService;
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
    public ResponseEntity<Void> like(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("id") UUID videoId
    ) {
        likeService.create(userId, videoId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<GetVideoLikesResponse> getVideoLikes(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("id") UUID videoId
    ) {
        var response = likeService.get(userId, videoId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<DeleteLikeResponse> deleteVideoLikes(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("id") UUID videoId
    ) {
        var response = likeService.delete(userId, videoId);

        return ResponseEntity.ok(response);
     }
}
