package com.shin.metadata.model;

import com.shin.metadata.model.enums.TranscodingStatus;
import com.shin.metadata.model.enums.UploadingStatus;
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
    private Integer uploadingProgress;
    private String failureReason;
    private String fileName;
    private Long durationSeconds;
    private Long fileSizeBytes;
    private String fileType;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    private TranscodingStatus transcodingStatus;

    @Enumerated(EnumType.STRING)
    private UploadingStatus uploadingStatus;


    public boolean isProcessed() {
       return this.uploadingProgress.equals(UploadingStatus.DONE) && this.transcodingStatus.equals(TranscodingStatus.DONE) && this.failureReason == null;
    }

}
