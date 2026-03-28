package com.shin.metadata.service;

import com.shin.metadata.dto.ProcessingProgress;

import java.util.UUID;

public interface ProcessingProgressService {

    ProcessingProgress getProgress(UUID videoId);

    void clearProgress(UUID videoId);

}
