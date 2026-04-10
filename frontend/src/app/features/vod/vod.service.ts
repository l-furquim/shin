import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import type { PlaybackEventRequest, WatchVodResponse } from './vod.types';

@Injectable({ providedIn: 'root' })
export class VodService {
  private readonly http = inject(HttpClient);

  watchVod(videoId: string): Observable<WatchVodResponse> {
    return this.http
      .get<WatchVodResponse>('/api/v1/vod', {
        params: { videoId },
        withCredentials: true, // required: CloudFront signed cookies are set via Set-Cookie
      })
      .pipe(catchError((error) => this.handleError(error, 'carregar vídeo')));
  }

  sendPlaybackEvent(event: PlaybackEventRequest): Observable<void> {
    return this.http
      .post<void>('/api/v1/vod/playback', event)
      .pipe(catchError((error) => this.handleError(error, 'registrar progresso')));
  }

  private handleError(error: unknown, operation: string): Observable<never> {
    const message =
      error instanceof HttpErrorResponse
        ? `Falha ao ${operation}. Status ${error.status}`
        : `Falha ao ${operation}.`;
    return throwError(() => new Error(message));
  }
}
