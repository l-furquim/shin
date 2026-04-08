package com.shin.comment.controller;

import com.shin.comment.dto.CommentListResponse;
import com.shin.comment.dto.CreateCommentRequest;
import com.shin.comment.dto.CreateCommentResponse;
import com.shin.comment.dto.UpdateCommentRequest;
import com.shin.comment.dto.UpdateCommentResponse;
import com.shin.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@RequestMapping("${api.version}/comments")
@RestController
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CreateCommentResponse> create(
            @RequestBody CreateCommentRequest createCommentRequest,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        final var response = this.commentService.createComment(userId.toString(), createCommentRequest);

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<CommentListResponse> list(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String parentId,
            @RequestParam(defaultValue = "20") int maxResults,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "html") String textFormat
    ) {
        List<String> ids = (id != null && !id.isBlank())
                ? Arrays.asList(id.split(","))
                : List.of();

        CommentListResponse response = commentService.listComments(ids, parentId, maxResults, pageToken, textFormat);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<UpdateCommentResponse> update(
            @RequestBody UpdateCommentRequest request,
            @PathVariable("commentId") UUID commentId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        final var response = this.commentService.updateComment(userId.toString(), commentId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable("commentId") UUID commentId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        this.commentService.deleteComment(userId.toString(), commentId);

        return ResponseEntity.noContent().build();
    }

}
