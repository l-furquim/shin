import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { concatMap, from, map, Observable, catchError, throwError } from 'rxjs';
import type {
  InitiateUploadRequest,
  InitiateUploadResponse,
  UploadChunkProgress,
  UploadChunkResponse,
  UploadChunksRequest,
} from './upload.types';

@Injectable({ providedIn: 'root' })
export class UploadService {
  private readonly http = inject(HttpClient);
  private readonly chunkSize = 5 * 1024 * 1024;

  initiateChunkedUpload(request: InitiateUploadRequest): Observable<InitiateUploadResponse> {
    const { userId, ...body } = request;

    return this.http
      .post<InitiateUploadResponse>('/api/v1/uploads/video/initiate', body, {
        headers: {
          'X-User-Id': userId,
        },
      })
      .pipe(catchError((error) => this.handleHttpError(error, 'iniciar upload')));
  }

  uploadChunks(request: UploadChunksRequest): Observable<UploadChunkProgress> {
    const chunkSequence = Array.from({ length: request.totalChunks }, (_, index) => index + 1);

    return from(chunkSequence).pipe(
      concatMap((chunkNumber) => this.uploadSingleChunk(request, chunkNumber)),
    );
  }

  private uploadSingleChunk(
    request: UploadChunksRequest,
    chunkNumber: number,
  ): Observable<UploadChunkProgress> {
    const start = (chunkNumber - 1) * this.chunkSize;
    const end = Math.min(start + this.chunkSize, request.file.size);
    const chunkBlob = request.file.slice(start, end);

    const formData = new FormData();
    formData.append('file', chunkBlob);

    const params = new HttpParams()
      .set('uploadId', request.uploadId)
      .set('chunkNumber', String(chunkNumber))
      .set('totalChunks', String(request.totalChunks));

    return this.http.post<UploadChunkResponse>('/api/v1/uploads/video/chunk', formData, { params }).pipe(
      map((response) => ({
        chunkNumber,
        totalChunks: request.totalChunks,
        progress: this.normalizeProgress(response.progress, chunkNumber, request.totalChunks),
      })),
      catchError((error) => this.handleHttpError(error, `enviar chunk ${chunkNumber}`)),
    );
  }

  private normalizeProgress(
    rawProgress: number,
    chunkNumber: number,
    totalChunks: number,
  ): number {
    if (Number.isFinite(rawProgress)) {
      return Math.max(0, Math.min(100, Math.round(rawProgress)));
    }

    return Math.round((chunkNumber / totalChunks) * 100);
  }

  private handleHttpError(error: unknown, operation: string): Observable<never> {
    const message =
      error instanceof HttpErrorResponse
        ? `Falha ao ${operation}. Status ${error.status}`
        : `Falha ao ${operation}.`;

    return throwError(() => new Error(message));
  }
}
