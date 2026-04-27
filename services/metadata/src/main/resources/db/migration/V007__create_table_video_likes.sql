CREATE TABLE IF NOT EXISTS video_likes (
    video_id   UUID NOT NULL,
    user_id    UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (video_id, user_id),

    CONSTRAINT fk_video_likes_video FOREIGN KEY (video_id) REFERENCES metadata.videos(id) ON DELETE CASCADE
);

CREATE INDEX idx_video_likes_video_id ON metadata.video_likes (video_id);
CREATE INDEX idx_video_likes_user_id  ON metadata.video_likes (user_id);
