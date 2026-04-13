package com.shin.comment.domain.gateway;

import java.util.Optional;
import java.util.UUID;

public interface CreatorGateway {

    Optional<CreatorInfo> findById(UUID id);

    record CreatorInfo(String displayName, String avatarUrl, String link) {}

}
