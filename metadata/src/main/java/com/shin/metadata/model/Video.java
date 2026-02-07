package com.shin.metadata.model;

import com.shin.metadata.model.enums.ProcessingStatus;
import com.shin.metadata.model.enums.VideoLanguage;
import com.shin.metadata.model.enums.VideoVisibility;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    private UUID videoId;

    private String title;

    private String description;

    private String accountId;

    private String videoKey;

    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    private VideoCategory videoCategory;

    @Enumerated(EnumType.STRING)
    private VideoVisibility visibility;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    @Enumerated(EnumType.STRING)
    private VideoLanguage defaultLanguage;

    private String publishedLocale;

    @Builder.Default
    private Boolean onlyForAdults = false;

    @ElementCollection
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private Long duration;

    @ElementCollection
    @Builder.Default
    private List<String> resolutions = new ArrayList<>();

    private LocalDateTime publishedAt;

    private LocalDateTime scheduledPublishAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    public void updateResolutions(List<String> newResolutions) {
        resolutions.clear();
        resolutions.addAll(newResolutions);
    }

    public void updateTags(List<String> newTags) {
        tags.clear();
        tags.addAll(newTags);
    }
}
