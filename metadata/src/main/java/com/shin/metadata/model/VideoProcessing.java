package com.shin.metadata.model;

import com.shin.metadata.model.enums.TranscodingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "video_processing")
public class VideoProcessing {

    @Id
    private UUID videoId;
    private String uploadKey;
    private Integer transcodingProgress;
    private String failureReason;
    private String fileName;
    private Long durationSeconds;
    private Long fileSizeBytes;
    private String fileType;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    private TranscodingStatus transcodingStatus;

    public boolean isProcessed() {
       return this.transcodingStatus.equals(TranscodingStatus.DONE) && this.failureReason == null;
    }

}
