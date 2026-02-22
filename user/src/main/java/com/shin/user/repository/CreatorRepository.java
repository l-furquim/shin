package com.shin.user.repository;

import com.shin.user.model.Creator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CreatorRepository extends JpaRepository<Creator, UUID> {

    @Modifying
    @Query("""
        UPDATE Creator c
        SET c.subscribersCount = GREATEST(0, c.subscribersCount + :delta)
        WHERE c.id = :creatorId
    """)
    int applySubscriberDelta(@Param("creatorId") UUID creatorId, @Param("delta") Long delta);

}
