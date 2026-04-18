import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import type { SearchVideosRequest, SearchVideosResponse } from './search.types';

@Injectable({ providedIn: 'root' })
export class SearchService {
  private readonly http = inject(HttpClient);

  searchVideos(request: SearchVideosRequest): Observable<SearchVideosResponse> {
    const params: Record<string, string | number | boolean> = {};

    if (request.q?.trim()) params['q'] = request.q.trim();
    if (request.tags?.length) params['tags'] = request.tags.join(',');
    if (request.language?.trim()) params['language'] = request.language.trim();
    if (request.category?.trim()) params['category'] = request.category.trim();
    if (request.dateFrom) params['dateFrom'] = request.dateFrom;
    if (request.dateTo) params['dateTo'] = request.dateTo;
    if (request.forAdults !== undefined) params['forAdults'] = request.forAdults;
    if (request.maxResults) params['maxResults'] = request.maxResults;
    if (request.pageToken) params['pageToken'] = request.pageToken;

    return this.http
      .get<SearchVideosResponse>('/api/v1/search', { params })
      .pipe(catchError((error) => this.handleHttpError(error, 'buscar vídeos')));
  }

  private handleHttpError(error: unknown, operation: string): Observable<never> {
    const message =
      error instanceof HttpErrorResponse
        ? `Falha ao ${operation}. Status ${error.status}`
        : `Falha ao ${operation}.`;
    return throwError(() => new Error(message));
  }
}
