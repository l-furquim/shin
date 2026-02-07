package com.shin.metadata.model;

import com.shin.metadata.model.enums.PlaylistVisibility;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "playlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Playlist {

    @Id
    private UUID id;

    private String name;

    private String description;

    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    private PlaylistVisibility visibility;

    @ElementCollection
    @Builder.Default
    private List<UUID> videos = new ArrayList<>();
}
