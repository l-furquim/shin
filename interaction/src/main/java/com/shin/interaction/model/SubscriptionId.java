package com.shin.interaction.model;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@EqualsAndHashCode()
@NoArgsConstructor
@Embeddable
public class SubscriptionId implements Serializable {

    private UUID followerId;

    private UUID channelId;

}
