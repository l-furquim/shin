import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { concatMap, from, map, Observable, catchError, throwError } from 'rxjs';
import type {
  CompleteUploadResponse,
  InitiateUploadRequest,
  InitiateUploadResponse,
  UploadChunkProgress,
  UploadChunksRequest,
} from './upload.types';

@Injectable({ providedIn: 'root' })
export class UploadService {
  private readonly http = inject(HttpClient);
  private readonly chunkSize = 5 * 1024 * 1024;

  initiateChunkedUpload(request: InitiateUploadRequest): Observable<InitiateUploadResponse> {
    return this.http
      .post<InitiateUploadResponse>('/api/v1/uploads/chunked', request)
      .pipe(catchError((error) => this.handleHttpError(error, 'iniciar upload')));
  }

  uploadChunks(request: UploadChunksRequest): Observable<UploadChunkProgress> {
    const indices = Array.from({ length: request.totalChunks }, (_, i) => i);
    return from(indices).pipe(
      concatMap((index) => this.uploadChunk(request, index)),
    );
  }

  completeChunkedUpload(uploadId: string): Observable<CompleteUploadResponse> {
    return this.http
      .post<CompleteUploadResponse>(`/api/v1/uploads/chunked/${uploadId}/complete`, null)
      .pipe(catchError((error) => this.handleHttpError(error, 'finalizar upload')));
  }

  private uploadChunk(request: UploadChunksRequest, index: number): Observable<UploadChunkProgress> {
    const { url } = request.chunks[index];
    const start = index * this.chunkSize;
    const end = Math.min(start + this.chunkSize, request.file.size);
    const blob = request.file.slice(start, end);

    return this.http
      .put(url, blob)
      .pipe(
        map(() => ({
          chunkNumber: index + 1,
          totalChunks: request.totalChunks,
          progress: Math.round(((index + 1) / request.totalChunks) * 100),
        })),
        catchError((error) => this.handleHttpError(error, `enviar chunk ${index + 1}`)),
      );
  }

  private handleHttpError(error: unknown, operation: string): Observable<never> {
    if (error instanceof HttpErrorResponse) {
      const detail =
        typeof error.error === 'object' && error.error?.message
          ? error.error.message
          : `Status ${error.status}`;
      return throwError(() => new Error(`Falha ao ${operation}. ${detail}`));
    }

    return throwError(() => new Error(`Falha ao ${operation}.`));
  }
}
