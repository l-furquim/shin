ALTER TABLE links
ADD COLUMN IF NOT EXISTS user_id UUID;

ALTER TABLE links
ALTER COLUMN user_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_links_user'
          AND table_name = 'links'
    ) THEN
        ALTER TABLE links
        ADD CONSTRAINT fk_links_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_links_user_id ON links(user_id);
