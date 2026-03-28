CREATE TABLE IF NOT EXISTS comments (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id   UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    parent_id  UUID,
    content    TEXT        NOT NULL,
    like_count INT         NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comments_video  FOREIGN KEY (video_id)  REFERENCES videos(id)   ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_video_id   ON comments (video_id);
CREATE INDEX idx_comments_user_id    ON comments (user_id);
CREATE INDEX idx_comments_parent_id  ON comments (parent_id);
