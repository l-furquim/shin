package com.shin.comment.service;

import com.shin.comment.dto.CommentListResponse;
import com.shin.comment.dto.CreateCommentRequest;
import com.shin.comment.dto.CreateCommentResponse;
import com.shin.comment.dto.UpdateCommentRequest;
import com.shin.comment.dto.UpdateCommentResponse;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    CreateCommentResponse createComment(String userId, CreateCommentRequest request);
    UpdateCommentResponse updateComment(String userId, UUID commentId, UpdateCommentRequest request);
    void deleteComment(String userId, UUID commentId);

    CommentListResponse listComments(
            List<String> ids,
            String parentId,
            int maxResults,
            String pageToken,
            String textFormat
    );

}
