import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';
import type { Resolution } from '@/features/videos/video.types';
import type { PlaybackEventRequest, WatchVodApiResponse, WatchVodResponse } from './vod.types';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class VodService {
  private readonly http = inject(HttpClient);

  watchVod(videoId: string, resolution: Resolution): Observable<WatchVodResponse> {
    const params: Record<string, string> = {
      videoId,
      resolutions: resolution,
    };

    return this.http
      .get<WatchVodApiResponse>('/api/v1/vod', {
        params,
        withCredentials: true, // required: CloudFront signed cookies are set via Set-Cookie
      })
      .pipe(
        map((response) => this.toWatchVodResponse(response, resolution)),
      )
      .pipe(catchError((error) => this.handleError(error, 'carregar vídeo')));
  }

  private toWatchVodResponse(response: WatchVodApiResponse, resolution: Resolution): WatchVodResponse {
    const manifestUrl = this.getManifestForResolution(response.manifests, resolution);

    return {
      videoDetails: response.videoDetails,
      playbackToken: response.playbackToken,
      manifestUrl: this.normalizeManifestUrl(manifestUrl),
    };
  }

  private getManifestForResolution(
    manifests: Array<Record<string, string>> | undefined,
    resolution: Resolution,
  ): string {
    if (!Array.isArray(manifests) || manifests.length === 0) {
      return '';
    }

    const exact = manifests.find((entry) => typeof entry?.[resolution] === 'string')?.[resolution];
    if (exact) {
      return exact;
    }

    const first = manifests[0];
    if (first) {
      const firstValue = Object.values(first).find((value): value is string => typeof value === 'string');
      if (firstValue) {
        return firstValue;
      }
    }

    return '';
  }

  sendPlaybackEvent(event: PlaybackEventRequest): Observable<void> {
    return this.http
      .post<void>('/api/v1/vod/playback', event, {
        withCredentials: true,
      })
      .pipe(catchError((error) => this.handleError(error, 'registrar progresso')));
  }

  private normalizeManifestUrl(manifestUrl: string | undefined): string {
    const raw = (manifestUrl ?? '').trim();
    if (!raw) {
      return '';
    }

    if (/^https?:\/\//i.test(raw)) {
      return raw;
    }

    const base = this.normalizeCloudfrontBase(environment.defaultCloudfrontUrl);
    if (!base) {
      return raw;
    }

    const normalizedRaw = raw.replace(/^\/+/, '');
    return `${base}/${normalizedRaw}`;
  }

  private normalizeCloudfrontBase(base: string | undefined): string {
    const normalized = (base ?? '').trim().replace(/\/+$/, '');
    if (!normalized) {
      return '';
    }

    if (normalized.startsWith('http://') || normalized.startsWith('https://')) {
      return normalized;
    }

    return `https://${normalized}`;
  }

  private handleError(error: unknown, operation: string): Observable<never> {
    const message =
      error instanceof HttpErrorResponse
        ? `Falha ao ${operation}. Status ${error.status}`
        : `Falha ao ${operation}.`;
    return throwError(() => new Error(message));
  }
}
