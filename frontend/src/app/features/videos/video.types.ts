import type { Tag } from '@/features/tags/tag.types';

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

export interface InitVideoResponse {
  videoId: string;
  status: string;
}

export interface VideoCategory {
  id: string;
  name: string;
  imageUrl: string;
}

export interface SearchVideoItem {
  id: string;
  title: string;
  description: string;
  visibility: VideoVisibility;
  creatorId: string;
  onlyForAdults: boolean;
  uploadKey: string;
  thumbnailUrl: string;
  videoCategory: VideoCategory;
  defaultLanguage: string;
  publishedLocale: string;
  tags: Tag[];
  duration: number;
  resolutions: string;
  likeCount: number;
  viewCount: number;
  publishedAt: string | null;
  scheduledPublishAt: string | null;
  createdAt: string;
  updatedAt: string;
  status: ProcessingStatus;
}

export interface SearchVideosResponse {
  videos: SearchVideoItem[];
}
