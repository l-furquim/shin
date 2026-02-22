CREATE TABLE video_likes (
    user_id UUID,
    video_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, video_id)
);

CREATE INDEX idx_video_likes_video_id ON video_likes (video_id);