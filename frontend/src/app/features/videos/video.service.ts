import { HttpClient, HttpErrorResponse, HttpResourceRequest } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import type {
  GetVideoProgressResponse,
  InitVideoResponse,
  PatchVideoRequest,
  SearchVideosRequest,
  SearchVideosResponse,
  VideoItem,
} from './video.types';

@Injectable({ providedIn: 'root' })
export class VideoService {
  private readonly http = inject(HttpClient);

  initVideo(): Observable<InitVideoResponse> {
    return this.http
      .post<InitVideoResponse>('/api/v1/videos/init', {})
      .pipe(catchError((error) => this.handleHttpError(error, 'iniciar video')));
  }

  getVideo(id: string, fields?: string): Observable<VideoItem> {
    const params: Record<string, string> = {};
    if (fields) params['fields'] = fields;

    return this.http
      .get<VideoItem>(`/api/v1/videos/${id}`, { params })
      .pipe(catchError((error) => this.handleHttpError(error, 'carregar vídeo')));
  }

  searchVideos(request: SearchVideosRequest): HttpResourceRequest {
    return {
      url: '/api/v1/videos',
      method: 'GET',
      params: {
        ...(request.forMine && { forMine: request.forMine }),
        ...(request.id && { id: request.id }),
        ...(request.channelId && { channelId: request.channelId }),
        ...(request.fields && { fields: request.fields }),
        ...(request.myRating && { myRating: request.myRating }),
        ...(request.categoryId && { categoryId: request.categoryId }),
        ...(request.cursor && { cursor: request.cursor }),
        ...(request.limit && { limit: request.limit }),
      },
    };
  }

  patchVideo(id: string, request: PatchVideoRequest): Observable<VideoItem> {
    return this.http
      .patch<VideoItem>(`/api/v1/videos/${id}`, request)
      .pipe(catchError((error) => this.handleHttpError(error, 'atualizar vídeo')));
  }

  listVideos(request: SearchVideosRequest): Observable<SearchVideosResponse> {
    const params: Record<string, string | number> = {};
    if (request.id) params['id'] = request.id;
    if (request.fields) params['fields'] = request.fields;
    if (request.myRating) params['myRating'] = request.myRating;
    if (request.categoryId) params['categoryId'] = request.categoryId;
    if (request.cursor) params['cursor'] = request.cursor;
    if (request.limit) params['limit'] = request.limit;

    return this.http
      .get<SearchVideosResponse>('/api/v1/videos', { params })
      .pipe(catchError((error) => this.handleHttpError(error, 'listar vídeos')));
  }

  getProgress(id: string): Observable<GetVideoProgressResponse> {
    return this.http
      .get<GetVideoProgressResponse>(`/api/v1/videos/${id}/progress`)
      .pipe(catchError((error) => this.handleHttpError(error, 'buscar progresso')));
  }

  publish(id: string) {
    return this.http
      .post(`/api/v1/videos/${id}/publish`, {})
      .pipe(catchError((error) => this.handleHttpError(error, 'publicar video')));
  }

  private handleHttpError(error: unknown, operation: string): Observable<never> {
    const message =
      error instanceof HttpErrorResponse
        ? `Falha ao ${operation}. Status ${error.status}`
        : `Falha ao ${operation}.`;
    return throwError(() => new Error(message));
  }
}
