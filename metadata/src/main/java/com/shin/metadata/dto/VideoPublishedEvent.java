package com.shin.metadata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoPublishedEvent {
    private UUID id;
    private String title;
    private String description;
    private String categoryName;
    private String channelAvatar;
    private String channelName;
    private Double duration;
    private String thumbnailUrl;
    private String videoLink;
    private String language;
    private boolean forAdults;
    private Set<String> tags;
    private LocalDateTime publishedAt;
    private String visibility;
}
