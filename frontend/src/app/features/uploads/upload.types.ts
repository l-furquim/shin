import type { Resolution } from '@/features/videos/video.types';

export interface PresignedChunk {
  chunkIndex: number;
  url: string;
  expiresAt: number;
}

export interface InitiateUploadRequest {
  fileName: string;
  fileSize: number;
  mimeType: string;
  resolutions: Resolution[];
}

export interface InitiateUploadResponse {
  uploadId: string;
  videoId: string;
  totalChunks: number;
  chunks: PresignedChunk[];
}

export interface UploadChunksRequest {
  file: File;
  uploadId: string;
  totalChunks: number;
  chunks: PresignedChunk[];
}

export interface UploadChunkProgress {
  chunkNumber: number;
  totalChunks: number;
  progress: number;
}

export interface CompleteUploadResponse {
  videoId: string;
  status: string;
}
