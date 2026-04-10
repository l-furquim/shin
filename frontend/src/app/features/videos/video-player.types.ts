import type { Resolution } from './video.types';

export interface PlayerSource {
  cdnUrl: string;
  processedPath: string;
  resolution: Resolution;
}

export interface DashEventPayload {
  mediaType?: string;
  newQuality?: number;
}

export interface DashPlayerHandle {
  getCurrentTime(): number;
  setSource(manifestUrl: string, options?: { startAt?: number; autoPlay?: boolean }): void;
  reset(): void;
}
