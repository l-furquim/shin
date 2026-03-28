package com.shin.metadata.service;

import com.shin.metadata.dto.SearchCommentRequest;
import com.shin.metadata.dto.SearchCommentResponse;

public interface CommentService {

    SearchCommentResponse search(
            SearchCommentRequest request
    );

}
