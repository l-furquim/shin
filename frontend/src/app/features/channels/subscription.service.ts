import { HttpClient, HttpResourceRequest } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly http = inject(HttpClient);

  getSubscribedChannels(cursor?: string, limit = 20): HttpResourceRequest {
    return {
      url: '/api/v1/subscriptions/me',
      method: 'GET',
      params: {
        limit,
        ...(cursor && { cursor }),
      },
    };
  }

  getSubscriptionFeed(cursor?: string, limit = 20): HttpResourceRequest {
    return {
      url: '/api/v1/subscriptions/me/feed',
      method: 'GET',
      params: {
        fields: 'contentDetails,statistics,thumbnails,channel',
        limit,
        ...(cursor && { cursor }),
      },
    };
  }

  unsubscribe(channelId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/subscriptions/${channelId}`);
  }
}
