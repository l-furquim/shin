import type { Resolution } from './video.types';

export interface PlayerSource {
  cdnUrl: string;
  videoId: string;
  resolution: Resolution;
}

export interface DashEventPayload {
  mediaType?: string;
  newQuality?: number;
}

export interface DashPlayerHandle {
  reset(): void;
}
