import type { Resolution } from '@/features/videos/video.types';

export interface InitiateUploadRequest {
  videoId: string;
  fileName: string;
  fileSize: number;
  contentType: string;
  resolutions: Resolution[];
  userId: string;
}

export interface InitiateUploadResponse {
  uploadId: string;
  totalChunks: number;
}

export interface UploadChunksRequest {
  file: File;
  uploadId: string;
  totalChunks: number;
}

export interface UploadChunkResponse {
  progress: number;
}

export interface UploadChunkProgress {
  chunkNumber: number;
  totalChunks: number;
  progress: number;
}
