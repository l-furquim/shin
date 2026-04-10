import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import type {
  CommentDto,
  CommentListResponse,
  CreateCommentRequest,
  ThreadListResponse,
} from './comment.types';

export interface ListCommentsParams {
  ids?: string[];
  parentId?: string;
  maxResults?: number;
  pageToken?: string;
}

@Injectable({ providedIn: 'root' })
export class CommentService {
  private readonly http = inject(HttpClient);

  listComments(params: ListCommentsParams): Observable<CommentListResponse> {
    const query: Record<string, string | number> = {
      maxResults: params.maxResults ?? 20,
      textFormat: 'html',
    };

    if (params.ids?.length) query['id'] = params.ids.join(',');
    if (params.parentId) query['parentId'] = params.parentId;
    if (params.pageToken) query['pageToken'] = params.pageToken;

    return this.http
      .get<CommentListResponse>('/api/v1/comments', { params: query })
      .pipe(catchError((error) => this.handleError(error, 'carregar comentários')));
  }

  listThreads(videoId: string, maxResults = 20, pageToken?: string): Observable<ThreadListResponse> {
    const query: Record<string, string | number> = { videoId, maxResults };
    if (pageToken) query['pageToken'] = pageToken;

    return this.http
      .get<ThreadListResponse>('/api/v1/threads', { params: query })
      .pipe(catchError((error) => this.handleError(error, 'carregar comentários')));
  }

  createComment(request: CreateCommentRequest): Observable<CommentDto> {
    return this.http
      .post<CommentDto>('/api/v1/comments', request)
      .pipe(catchError((error) => this.handleError(error, 'publicar comentário')));
  }

  deleteComment(commentId: string): Observable<void> {
    return this.http
      .delete<void>(`/api/v1/comments/${commentId}`)
      .pipe(catchError((error) => this.handleError(error, 'deletar comentário')));
  }

  private handleError(error: unknown, operation: string): Observable<never> {
    const message =
      error instanceof HttpErrorResponse
        ? `Falha ao ${operation}. Status ${error.status}`
        : `Falha ao ${operation}.`;
    return throwError(() => new Error(message));
  }
}
