package com.shin.user.repository;

import com.shin.user.model.Subscription;
import com.shin.user.model.SubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, SubscriptionId> {

    long countByIdChannelId(UUID channelId);

}
