CREATE TABLE IF NOT EXISTS playlists_videos (
    playlist_id UUID NOT NULL,
    video_id UUID NOT NULL,
    video_order INTEGER NOT NULL,
    CONSTRAINT fk_playlist FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    CONSTRAINT fk_video FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE
);

CREATE INDEX idx_playlists_videos_playlist_order ON playlists_videos(playlist_id, video_order);