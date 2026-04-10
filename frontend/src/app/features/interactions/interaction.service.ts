import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import type { ReactionResponse, ReactionType } from './interaction.types';

@Injectable({ providedIn: 'root' })
export class InteractionService {
  private readonly http = inject(HttpClient);

  react(videoId: string, type: ReactionType): Observable<ReactionResponse> {
    return this.http
      .post<ReactionResponse>(`/api/v1/reactions`, null, {
        params: { type, videoId },
      })
      .pipe(catchError((error) => this.handleError(error, 'reagir ao vídeo')));
  }

  removeReaction(videoId: string, type: ReactionType): Observable<ReactionResponse> {
    return this.http
      .delete<ReactionResponse>(`/api/v1/reactions`, {
        params: { type, videoId },
      })
      .pipe(catchError((error) => this.handleError(error, 'remover reação')));
  }

  private handleError(error: unknown, operation: string): Observable<never> {
    const message =
      error instanceof HttpErrorResponse
        ? `Falha ao ${operation}. Status ${error.status}`
        : `Falha ao ${operation}.`;
    return throwError(() => new Error(message));
  }
}
