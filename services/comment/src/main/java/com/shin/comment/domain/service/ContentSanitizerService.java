package com.shin.comment.domain.service;

import com.shin.comment.domain.exceptions.InvalidCommentContentException;

public interface ContentSanitizerService {

    String sanitize(String content) throws InvalidCommentContentException;
    String format(String content);

}
