import type { PageInfo } from '@/shared/core/request.types';

export interface SubscribedChannel {
  channelId: string;
  name: string;
  avatarUrl: string;
  subscribersCount: number;
  subscribedAt: string;
}

export interface SubscriptionsResponse {
  nextPageToken: string;
  prevPageToken: string;
  pageInfo: PageInfo;
  items: SubscribedChannel[];
}
