import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import type { Tag, TagSearchResponse } from './tag.types';

@Injectable({ providedIn: 'root' })
export class TagService {
  private readonly http = inject(HttpClient);

  searchTags(query: string): Observable<TagSearchResponse> {
    const params: Record<string, string> = {};
    if (query) params['q'] = query;

    return this.http
      .get<TagSearchResponse>('/api/v1/tags', { params })
      .pipe(catchError((error) => this.handleHttpError(error, 'buscar tags')));
  }

  createTag(name: string): Observable<Tag> {
    return this.http
      .post<Tag>('/api/v1/tags', { name })
      .pipe(catchError((error) => this.handleHttpError(error, 'criar tag')));
  }

  private handleHttpError(error: unknown, operation: string): Observable<never> {
    const message =
      error instanceof HttpErrorResponse
        ? `Falha ao ${operation}. Status ${error.status}`
        : `Falha ao ${operation}.`;
    return throwError(() => new Error(message));
  }
}
