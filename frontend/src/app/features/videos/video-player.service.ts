import { isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import type { Resolution } from './video.types';
import type { DashEventPayload, DashPlayerHandle, PlayerSource } from './video-player.types';

type DashModule = {
  MediaPlayer: {
    (): {
      create(): DashPlayer;
      events: Record<string, string>;
    };
    events: Record<string, string>;
  };
};

type DashPlayer = {
  initialize(videoElement: HTMLVideoElement, manifestUrl: string, autoPlay: boolean): void;
  on(event: string, listener: (payload?: unknown) => void): void;
  updateSettings(settings: object): void;
  extend(type: string, factory: () => object, override?: boolean): void;
  reset(): void;
};

export interface AttachPlayerOptions {
  videoElement: HTMLVideoElement;
  manifestUrl: string;
  withCredentials?: boolean;
  onStreamInitialized: () => void;
  onError: () => void;
  onQualityChanged: (qualityLabel: string) => void;
}

@Injectable({ providedIn: 'root' })
export class VideoPlayerService {
  private readonly platformId = inject(PLATFORM_ID);

  buildManifestUrl(source: PlayerSource): string {
    const normalizedCdnUrl = this.normalizeCdnUrl(source?.cdnUrl);
    const normalizedProcessedPath = this.normalizeProcessedPath(source?.processedPath);
    const resolution = source?.resolution ?? '720p';

    if (!normalizedCdnUrl || !normalizedProcessedPath) {
      return '';
    }

    return `${normalizedCdnUrl}/${normalizedProcessedPath}/${resolution}/manifest.mpd`;
  }

  isResolution(value: string): value is Resolution {
    return value === '360p' || value === '480p' || value === '720p' || value === '1080p';
  }

  getBufferLevel(videoElement: HTMLVideoElement): string {
    if (videoElement.buffered.length === 0) return '-';

    const bufferedEnd = videoElement.buffered.end(videoElement.buffered.length - 1);
    const bufferLevel = Math.max(0, bufferedEnd - videoElement.currentTime);
    return `${bufferLevel.toFixed(1)}s`;
  }

  async attachPlayer(options: AttachPlayerOptions): Promise<DashPlayerHandle> {
    if (!isPlatformBrowser(this.platformId)) {
      throw new Error('Player indisponível fora do browser.');
    }

    const dashImport = await import('dashjs');
    const dash = ((dashImport as { default?: DashModule }).default ?? dashImport) as DashModule;
    const mediaPlayer = dash.MediaPlayer().create();

    mediaPlayer.initialize(options.videoElement, options.manifestUrl, true);

    // Cloud front signed cookies integration.
    if (options.withCredentials) {
      mediaPlayer.extend(
        'RequestModifier',
        () => ({
          modifyRequestHeader(xhr: XMLHttpRequest) {
            xhr.withCredentials = true;
            return xhr;
          },
        }),
        true,
      );
    }

    mediaPlayer.on(dash.MediaPlayer.events['STREAM_INITIALIZED'], () => {
      options.onStreamInitialized();
    });

    mediaPlayer.on(dash.MediaPlayer.events['ERROR'], () => {
      options.onError();
    });

    mediaPlayer.on(dash.MediaPlayer.events['QUALITY_CHANGE_RENDERED'], (payload?: unknown) => {
      const event = payload as DashEventPayload;
      if (event.mediaType === 'video' && typeof event.newQuality === 'number') {
        options.onQualityChanged(`Nível ${event.newQuality}`);
      }
    });

    mediaPlayer.updateSettings({
      streaming: {
        abr: { autoSwitchBitrate: { video: true } },
        buffer: { fastSwitchEnabled: true },
      },
    });

    return { reset: () => mediaPlayer.reset() };
  }

  private normalizeCdnUrl(url: string | undefined): string {
    if (!url) return '';
    const trimmed = url.trim().replace(/\/$/, '');
    if (!trimmed) return '';
    if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) return trimmed;
    return `https://${trimmed}`;
  }

  private normalizeProcessedPath(path: string | undefined): string {
    if (!path) return '';
    const trimmed = path.trim().replace(/^\/+|\/+$/g, '');
    if (!trimmed) return '';
    return trimmed.startsWith('videos/') ? trimmed : `videos/${trimmed}`;
  }
}
