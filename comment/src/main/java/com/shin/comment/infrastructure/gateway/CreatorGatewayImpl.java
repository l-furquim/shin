package com.shin.comment.infrastructure.gateway;

import com.shin.comment.domain.gateway.CreatorGateway;
import com.shin.comment.infrastructure.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class CreatorGatewayImpl implements CreatorGateway {

    private final UserServiceClient userServiceClient;

    @Override
    public Optional<CreatorInfo> findById(UUID id) {
        try {
            var response = userServiceClient.getCreatorById(id);
            if (response == null) return Optional.empty();
            return Optional.of(new CreatorInfo(response.displayName(), response.avatar(), response.link()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
