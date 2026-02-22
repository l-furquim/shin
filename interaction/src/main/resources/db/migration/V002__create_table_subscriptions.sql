CREATE TABLE subscriptions (
    follower_id UUID,
    channel_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (follower_id, channel_id)
);

CREATE INDEX idx_subscriptions_channel_id ON subscriptions (channel_id);