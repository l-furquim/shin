import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import type { CategorySearchResponse } from './category.types';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);

  searchCategories(query: string): Observable<CategorySearchResponse> {
    const params: Record<string, string> = {};
    if (query) params['q'] = query;

    return this.http
      .get<CategorySearchResponse>('/api/v1/categories', { params })
      .pipe(catchError((error) => this.handleHttpError(error, 'buscar categorias')));
  }

  private handleHttpError(error: unknown, operation: string): Observable<never> {
    const message =
      error instanceof HttpErrorResponse
        ? `Falha ao ${operation}. Status ${error.status}`
        : `Falha ao ${operation}.`;
    return throwError(() => new Error(message));
  }
}
