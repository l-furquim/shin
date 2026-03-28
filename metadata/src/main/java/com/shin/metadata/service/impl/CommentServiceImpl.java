package com.shin.metadata.service.impl;

import com.shin.metadata.dto.SearchCommentRequest;
import com.shin.metadata.dto.SearchCommentResponse;
import com.shin.metadata.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentServiceImpl implements CommentService {
    @Override
    public SearchCommentResponse search(SearchCommentRequest request) {
        return null;
    }
}
