import type { PageInfo } from '@/shared/core/request.types';

export interface SearchVideosRequest {
  q?: string;
  tags?: string[];
  language?: string;
  category?: string;
  dateFrom?: string;
  dateTo?: string;
  forAdults?: boolean;
  maxResults?: number;
  pageToken?: string;
}

export interface SearchVideoItem {
  id: string;
  title: string;
  description: string;
  categoryName: string;
  channelName: string;
  channelAvatar: string;
  duration: number;
  thumbnailUrl: string;
  videoLink: string;
  language: string;
  forAdults: boolean;
  tags: string[];
  score: number;
  publishedAt: string;
}

export interface SearchVideosResponse {
  nextPageToken: string | null;
  pageInfo: PageInfo;
  results: SearchVideoItem[];
}
