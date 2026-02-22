CREATE TABLE subscriptions (
    follower_id UUID NOT NULL,
    channel_id  UUID NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (follower_id, channel_id),

    CONSTRAINT fk_subscriptions_follower FOREIGN KEY (follower_id) REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_channel  FOREIGN KEY (channel_id)  REFERENCES creators(id) ON DELETE CASCADE
);

CREATE INDEX idx_subscriptions_channel_id  ON subscriptions (channel_id);
CREATE INDEX idx_subscriptions_follower_id ON subscriptions (follower_id);
