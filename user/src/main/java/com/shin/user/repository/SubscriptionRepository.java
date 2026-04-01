package com.shin.user.repository;

import com.shin.user.model.Subscription;
import com.shin.user.model.SubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, SubscriptionId> {

    long countByIdChannelId(UUID channelId);

    boolean existsByIdFollowerIdAndIdChannelId(UUID followerId, UUID channelId);

    @Modifying
    @Query(value = """
        INSERT INTO "user".subscriptions (follower_id, channel_id)
        VALUES (:followerId, :channelId)
        ON CONFLICT DO NOTHING
    """, nativeQuery = true)
    int insertIgnore(@Param("followerId") UUID followerId, @Param("channelId") UUID channelId);

    @Modifying
    @Query("""
        DELETE FROM Subscription s
        WHERE s.id.followerId = :followerId
          AND s.id.channelId = :channelId
    """)
    int deleteExisting(@Param("followerId") UUID followerId, @Param("channelId") UUID channelId);

}
