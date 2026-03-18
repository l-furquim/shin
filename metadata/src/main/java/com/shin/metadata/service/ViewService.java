package com.shin.metadata.service;

import java.util.Optional;
import java.util.UUID;

public interface ViewService {

    void increaseView(
            UUID videoId
    );

    Optional<Long> getVideoViews(UUID videoId);

}
