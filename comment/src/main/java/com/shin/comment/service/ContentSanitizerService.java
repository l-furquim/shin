package com.shin.comment.service;

import com.shin.comment.exception.InvalidCommentContentException;

public interface ContentSanitizerService {

    String sanitize(String content) throws InvalidCommentContentException;
    String format(String content);

}
