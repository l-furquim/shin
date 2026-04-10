import { isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import type { DashEventPayload, DashPlayerHandle } from './video-player.types';

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
  attachSource(manifestUrl: string): void;
  play(): void;
  getVideoElement(): HTMLVideoElement | null;
  reset(): void;
};

export interface AttachPlayerOptions {
  videoElement: HTMLVideoElement;
  manifestUrl: string;
  autoPlay?: boolean;
  startAt?: number;
  withCredentials?: boolean;
  onStreamInitialized: () => void;
  onError: () => void;
  onQualityChanged: (qualityLabel: string) => void;
}

@Injectable({ providedIn: 'root' })
export class VideoPlayerService {
  private readonly platformId = inject(PLATFORM_ID);

  async attachPlayer(options: AttachPlayerOptions): Promise<DashPlayerHandle> {
    if (!isPlatformBrowser(this.platformId)) {
      throw new Error('Player indisponível fora do browser.');
    }

    const dashImport = await import('dashjs');
    const dash = ((dashImport as { default?: DashModule }).default ?? dashImport) as DashModule;
    const mediaPlayer = dash.MediaPlayer().create();
    let pendingSeekSeconds = options.startAt;

    mediaPlayer.initialize(options.videoElement, options.manifestUrl, options.autoPlay ?? true);

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
      const videoElement = mediaPlayer.getVideoElement();
      if (
        videoElement &&
        typeof pendingSeekSeconds === 'number' &&
        Number.isFinite(pendingSeekSeconds) &&
        pendingSeekSeconds > 0
      ) {
        try {
          videoElement.currentTime = pendingSeekSeconds;
        } catch {
          // ignore invalid seek attempts
        }
      }
      pendingSeekSeconds = undefined;
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

    return {
      getCurrentTime: () => mediaPlayer.getVideoElement()?.currentTime ?? 0,
      setSource: (manifestUrl: string, sourceOptions?: { startAt?: number; autoPlay?: boolean }) => {
        pendingSeekSeconds = sourceOptions?.startAt;
        mediaPlayer.attachSource(manifestUrl);

        if (sourceOptions?.autoPlay) {
          try {
            mediaPlayer.play();
          } catch {
            // autoplay may fail due to browser policy
          }
        }
      },
      reset: () => mediaPlayer.reset(),
    };
  }
}
