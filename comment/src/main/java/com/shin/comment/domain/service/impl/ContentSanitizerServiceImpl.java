package com.shin.comment.domain.service.impl;

import com.shin.comment.domain.exceptions.InvalidCommentContentException;
import com.shin.comment.domain.service.ContentSanitizerService;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;

@Service
public class ContentSanitizerServiceImpl implements ContentSanitizerService {

    @Override
    public String sanitize(String content) throws InvalidCommentContentException {
        final var policy = new HtmlPolicyBuilder().toFactory();
        final var contentSanitized = policy.sanitize(content);

        if (contentSanitized.isBlank() || contentSanitized.length() > 1000) {
            throw new InvalidCommentContentException();
        }

        return contentSanitized;
    }

    @Override
    public String format(String content) {
        PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);
        return policy.sanitize(content);
    }
}
