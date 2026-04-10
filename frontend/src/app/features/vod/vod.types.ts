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
  videoDetails: WatchVodVideoDetails;
  manifestUrl: string;
  playbackToken: string;
}

export interface PlaybackEventRequest {
  playbackSessionToken: string;
  watchTimeSeconds: number;
  currentPositionSeconds: number;
  totalDurationSeconds: number;
}
