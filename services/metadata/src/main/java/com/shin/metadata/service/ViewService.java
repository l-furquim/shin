package com.shin.metadata.service;

import java.util.Optional;
import java.util.Map;
import java.util.UUID;

public interface ViewService {

    void increaseView(UUID videoId, String viewerKey);

    Optional<Long> getPendingViews(UUID videoId);

    long getEffectiveVideoViews(UUID videoId, Long persistedViews);

    Map<UUID, Long> getEffectiveVideoViews(Map<UUID, Long> persistedViewsByVideoId);

}
