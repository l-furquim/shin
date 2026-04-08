package com.shin.comment.controller;

import com.shin.comment.dto.ThreadListResponse;
import com.shin.comment.service.ThreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RequestMapping("${api.version}/threads")
@RestController
public class ThreadController {

    private final ThreadService threadService;

    @GetMapping
    public ResponseEntity<ThreadListResponse> list(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String videoId,
            @RequestParam(required = false) String allThreadsRelatedToChannelId,
            @RequestParam(defaultValue = "20") int maxResults,
            @RequestParam(defaultValue = "time") String order,
            @RequestParam(required = false) String pageToken,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        List<String> ids = (id != null && !id.isBlank())
                ? Arrays.asList(id.split(","))
                : List.of();

        ThreadListResponse response = threadService.listThreads(
                ids, videoId, allThreadsRelatedToChannelId,
                maxResults, order, pageToken);

        return ResponseEntity.ok(response);
    }
}
