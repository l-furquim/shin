CREATE TABLE comments (
    id PRIMARY KEY UUID DEFAULT uuidv7(),
    video_id UUID,
    user_id UUID,
    parent_id UUID,
    root_id UUID,
    depth INT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);