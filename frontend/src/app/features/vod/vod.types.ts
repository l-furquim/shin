export interface WatchVodVideoDetails {
  id: string;
  creatorId: string;
  title: string;
  duration: number;
  description: string;
  visibility: string;
  status: string;
}

export interface WatchVodResponse {
  lastWatch: LastWatch;
  videoDetails: WatchVodVideoDetails;
  manifestUrl: string;
  playbackToken: string;
}

export interface WatchVodApiResponse {
  lastWatch: LastWatch;
  videoDetails: WatchVodVideoDetails;
  manifests: Array<Record<string, string>>;
  playbackToken: string;
}

export interface LastWatch {
  accumulatedWatchTime: number;
  viewCounted: boolean;
}

export interface PlaybackEventRequest {
  playbackSessionToken: string;
  watchTimeSeconds: number;
  currentPositionSeconds: number;
  totalDurationSeconds: number;
}
