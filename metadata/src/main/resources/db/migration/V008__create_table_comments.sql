CREATE TABLE IF NOT EXISTS comments (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    video_id   UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    parent_id  UUID,
    root_id    UUID,
    content    TEXT        NOT NULL,
    depth      INTEGER     NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_comments_video  FOREIGN KEY (video_id)  REFERENCES videos(id)   ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_root   FOREIGN KEY (root_id)   REFERENCES comments(id) ON DELETE CASCADE,

    CONSTRAINT chk_comments_depth CHECK (depth >= 0)
);

CREATE INDEX idx_comments_video_id   ON comments (video_id);
CREATE INDEX idx_comments_user_id    ON comments (user_id);
CREATE INDEX idx_comments_parent_id  ON comments (parent_id);
CREATE INDEX idx_comments_root_id    ON comments (root_id);
