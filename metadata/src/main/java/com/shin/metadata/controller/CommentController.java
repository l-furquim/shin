package com.shin.metadata.controller;

import com.shin.metadata.dto.SearchCommentRequest;
import com.shin.metadata.dto.SearchCommentResponse;
import com.shin.metadata.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RequestMapping("${api.version}/comments")
@RestController
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<SearchCommentResponse> search(
            @RequestParam String fields,
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Long limit,
            @RequestParam(required = false) String textFormat
    ) {
        final var response = commentService.search(new SearchCommentRequest(
                fields,
                id,
                parentId,
                limit,
                cursor,
                textFormat
        ));

        return ResponseEntity.ok(response);
    }

}
