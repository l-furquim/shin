package com.shin.interaction.controller;

import com.shin.interaction.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.version}/reactions")
public class BatchReactionController {

    private final ReactionService reactionService;

    @GetMapping("/batch")
    public ResponseEntity<Map<String, String>> getBatchReactions(
            @RequestParam("videoIds") List<UUID> videoIds,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(reactionService.getBatchReactions(videoIds, userId));
    }
}
