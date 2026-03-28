import type { Resolution } from '@/features/videos/video.types';

export interface InitiateUploadRequest {
  videoId?: string;
  fileName: string;
  fileSize: number;
  contentType: string;
  resolutions: Resolution[];
  userId: string;
}

export interface InitiateUploadResponse {
  uploadId: string;
  videoId: string;
  chunkSize: number;
  totalChunks: number;
  resolutions: Resolution[];
}

export interface UploadChunksRequest {
  file: File;
  uploadId: string;
  totalChunks: number;
}

export interface UploadChunkResponse {
  uploadId: string;
  chunkNumber: number;
  uploaded: boolean;
  progress: number;
}

export interface CompleteUploadResponse {
  videoId: string;
  status: string;
}

export interface UploadChunkProgress {
  chunkNumber: number;
  totalChunks: number;
  progress: number;
}
