import type { PageInfo } from '@/shared/core/request.types';

export type Resolution = '360p' | '480p' | '720p' | '1080p';
export type VideoVisibility = 'PUBLIC' | 'PRIVATE' | 'NOT_LISTED';

export type TranscodingStatus = 'failed' | 'queued' | 'processing' | 'done';

export type ProcessingStatus =
  | 'UPLOADING'
  | 'UPLOADED'
  | 'PROCESSING'
  | 'PROCESSED'
  | 'DRAFT'
  | 'EXPIRED'
  | 'FAILED'
  | string;

export interface Thumbnail {
  url: string;
  width: number;
  height: number;
}

interface ContentDetails {
  resolutions: string;
  duration: number;
  processedPath?: string;
  defaultLanguage: string;
  publishedLocale: string;
  onlyForAdults: boolean;
}

interface FileDetails {
  fileName: string;
  fileSize: number;
  fileType: string;
}

interface ProcessingDetails {
  transcodingStatus: TranscodingStatus;
  transcodingFailureReason?: string;
  transcodingProgress: number;
}

interface Statistics {
  likeCount: number;
  viewCount: number;
  commentCount: number;
}

export interface ChannelDetails {
  id: string;
  name: string;
  avatarUrl: string;
}

export interface InitVideoResponse {
  videoId: string;
  status: string;
}

export interface VideoCategory {
  id: string;
  name: string;
  imageUrl: string;
}

export interface VideoItem {
  id: string;
  title: string;
  description: string;
  visibility: VideoVisibility;
  categoryId: string;
  thumbnails: Record<string, Thumbnail>;
  contentDetails?: ContentDetails;
  statistics?: Statistics;
  likedByMe: boolean;
  fileDetails?: FileDetails;
  processingDetails?: ProcessingDetails;
  channel: ChannelDetails;
  tags: Set<string>;
  publishedAt: Date;
  scheduledPublishAt: Date;
  createdAt: Date;
}

export interface SearchVideosRequest {
  id?: string;
  channelId?: string;
  fields?: string;
  myRating?: 'liked' | 'desliked';
  forMine?: boolean;
  categoryId?: string;
  cursor?: string;
  limit?: number;
}

export interface SearchVideosResponse {
  nextPageToken: string;
  prevPageToken: string;
  pageInfo: PageInfo;
  items: VideoItem[];
}

export interface TagIdentifier {
  name: string;
}

export interface PatchVideoRequest {
  title?: string;
  description?: string;
  categoryId?: number;
  defaultLanguage?: string;
  onlyForAdults?: boolean;
  tagsToAdd?: TagIdentifier[];
  tagsToRemove?: TagIdentifier[];
}

export interface GetVideoProgressResponse {
  transcodingProgress: number;
  failureReason: string;
  transcodingStatus: TranscodingStatus;
  fileSizeBytes: number;
  startedAt: Date;
}
