package com.shin.metadata.model;

import com.shin.metadata.model.enums.ProcessingStatus;
import com.shin.metadata.model.enums.VideoLanguage;
import com.shin.metadata.model.enums.VideoVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "videos", schema = "metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    private UUID id;

    private String title;

    private String description;

    private UUID creatorId;

    private String uploadKey;

    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
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

    @ManyToMany
    @JoinTable(
            name = "videos_tags",
            schema = "metadata",
            joinColumns = @JoinColumn(
                    name = "video_id",
                    referencedColumnName = "id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "tag_id",
                    referencedColumnName = "id"
            )
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    private Double duration;

    private String resolutions;

    private Long likeCount;

    private LocalDateTime publishedAt;

    private LocalDateTime scheduledPublishAt;

    private LocalDateTime expiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
    }

    public void updateResolutions(String newResolutions) {
        this.resolutions = newResolutions;
    }

    public void updateTags(Set<Tag> newTags) {
        tags.clear();
        tags.addAll(newTags);
    }
}
