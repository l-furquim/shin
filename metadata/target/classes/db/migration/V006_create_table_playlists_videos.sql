CREATE TABLE IF NOT EXISTS playlists_videos (
    playlist_id UUID NOT NULL,
    video_id UUID NOT NULL,
    PRIMARY KEY (playlist_id, video_id),
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE
);