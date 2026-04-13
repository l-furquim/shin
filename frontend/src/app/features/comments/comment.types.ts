import type { PageInfo } from '@/shared/core/request.types';

export interface CommentDto {
  id: string;
  parentId: string | null;
  videoId: string;
  authorId: string;
  authorDisplayName: string | null;
  authorAvatarUrl: string | null;
  authorUrl: string | null;
  textDisplay: string;
  textOriginal: string;
  likeCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CommentListResponse {
  nextPageToken: string | null;
  pageInfo: PageInfo;
  items: CommentDto[];
}

export interface CreateCommentRequest {
  videoId: string;
  channelId: string;
  content: string;
  parentId?: string;
}

export interface ThreadDto {
  id: string;
  videoId: string;
  channelId: string;
  authorId: string;
  authorDisplayName: string | null;
  authorAvatarUrl: string | null;
  authorUrl: string | null;
  totalReplyCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ThreadListResponse {
  nextPageToken: string | null;
  pageInfo: PageInfo;
  items: ThreadDto[];
}
