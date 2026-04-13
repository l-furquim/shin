ALTER TABLE metadata.videos
    ADD COLUMN IF NOT EXISTS comment_count        BIGINT  NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS dislike_count        BIGINT  NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS creator_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS creator_avatar_url   VARCHAR(512);
