package com.shin.streaming.dto;

import java.util.UUID;

public record ViewCountEvent(UUID videoId, UUID userId, String viewerKey) {
}
