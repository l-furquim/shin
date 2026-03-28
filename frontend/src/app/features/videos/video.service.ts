import { HttpClient, HttpErrorResponse, HttpResourceRequest } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import type { InitVideoResponse, SearchVideosRequest, SearchVideosResponse } from './video.types';

@Injectable({ providedIn: 'root' })
export class VideoService {
  private readonly http = inject(HttpClient);

  initVideo(userId: string): Observable<InitVideoResponse> {
    return this.http
      .post<InitVideoResponse>(
        '/api/v1/videos/init',
        {},
        {
          headers: {
            'X-User-Id': userId,
          },
        },
      )
      .pipe(catchError((error) => this.handleHttpError(error, 'iniciar video')));
  }

  searchVideos(request: SearchVideosRequest): HttpResourceRequest {
    return {
      url: '/api/v1/videos/search',
      method: 'GET',
      params: {
        ...(request.id && { id: request.id }),
        ...(request.fields && { fields: request.fields }),
        ...(request.myRating && { myRating: request.myRating }),
        ...(request.categoryId && { categoryId: request.categoryId }),
        ...(request.cursor && { cursor: request.cursor }),
        ...(request.limit && { limit: request.limit }),
      },
    };
  }

  private handleHttpError(error: unknown, operation: string): Observable<never> {
    const message =
      error instanceof HttpErrorResponse
        ? `Falha ao ${operation}. Status ${error.status}`
        : `Falha ao ${operation}.`;

    return throwError(() => new Error(message));
  }
}
