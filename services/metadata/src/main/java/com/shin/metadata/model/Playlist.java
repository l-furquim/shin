package com.shin.metadata.model;

import com.shin.metadata.model.enums.PlaylistVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "playlists", schema = "metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Playlist {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaylistVisibility visibility;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "playlists_videos",
        schema = "metadata",
        joinColumns = @JoinColumn(name = "playlist_id")
    )
    @Column(name = "video_id", nullable = false)
    @OrderColumn(name = "video_order")
    @Builder.Default
    private List<UUID> videos = new ArrayList<>();
}
