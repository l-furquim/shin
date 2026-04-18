CREATE TABLE video_processing (
    video_id UUID PRIMARY KEY,
    upload_key VARCHAR(255),
    transcoding_progress INT,
    uploading_progress INT,
    transcoding_status VARCHAR(255),
    uploading_status VARCHAR(255),
    failure_reason VARCHAR(255),
    file_name  VARCHAR(255),
    duration_seconds BIGINT,
    file_size_bytes BIGINT,
    file_type  VARCHAR(100),
    started_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
