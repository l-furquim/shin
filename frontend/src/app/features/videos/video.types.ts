import type { PageInfo } from '@/shared/core/request.types';

export type Resolution = '360p' | '480p' | '720p' | '1080p';
export type VideoVisibility = 'PUBLIC' | 'PRIVATE' | 'NOT_LISTED';

export type ProcessingStatus =
  | 'UPLOADING'
  | 'UPLOADED'
  | 'PROCESSING'
  | 'PROCESSED'
  | 'DRAFT'
  | 'EXPIRED'
  | 'FAILED'
  | string;

interface Thumbnail {
  url: string;
  width: number;
  height: number;
}

interface ContentDetails {
  resolutions: string;
  duration: number;
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
  processingStatus: ProcessingStatus;
  processingFailureReason?: string;
  processingProgress: number;
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
  thumbnails: Map<string, Thumbnail>;
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
  fields?: string;
  myRating?: 'liked' | 'desliked';
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
