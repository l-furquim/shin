import { inject, Injectable, OnDestroy } from '@angular/core';
import { EMPTY } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { VodService } from './vod.service';

interface TrackerConfig {
  token: string;
  duration: number;
}

@Injectable()
export class PlaybackTrackerService implements OnDestroy {
  private readonly vodService = inject(VodService);

  private config: TrackerConfig | null = null;
  private accumulatedSeconds = 0;
  private lastTickAt: number | null = null;
  private currentPosition = 0;
  private active = false;
  private intervalId: ReturnType<typeof setInterval> | null = null;

  private readonly FLUSH_INTERVAL_MS = 5_000;
  private readonly MAX_TICK_GAP_SEC = 2;

  start(config: TrackerConfig): void {
    this.stop();
    this.config = config;
    this.accumulatedSeconds = 0;
    this.currentPosition = 0;
    this.active = false;
    this.lastTickAt = null;
    this.intervalId = setInterval(() => this.flush(), this.FLUSH_INTERVAL_MS);
  }

  onPlay(): void {
    this.active = true;
    this.lastTickAt = performance.now();
  }

  onTimeUpdate(currentTime: number): void {
    if (!this.active || this.lastTickAt === null) return;

    const now = performance.now();
    const elapsedSec = Math.min((now - this.lastTickAt) / 1000, this.MAX_TICK_GAP_SEC);
    this.accumulatedSeconds += elapsedSec;
    this.lastTickAt = now;
    this.currentPosition = currentTime;
  }

  onPause(): void {
    this.active = false;
    this.lastTickAt = null;
    this.flush();
  }

  onEnded(): void {
    this.active = false;
    this.lastTickAt = null;
    this.flush();
    this.clearInterval();
  }

  stop(): void {
    this.active = false;
    this.lastTickAt = null;
    if (this.accumulatedSeconds >= 1) {
      this.flush();
    }
    this.clearInterval();
    this.config = null;
    this.accumulatedSeconds = 0;
    this.currentPosition = 0;
  }

  private flush(): void {
    if (!this.config || this.accumulatedSeconds < 1) return;

    const watchTime = Math.round(this.accumulatedSeconds);
    const position = Math.round(this.currentPosition);
    this.accumulatedSeconds = 0;

    this.vodService
      .sendPlaybackEvent({
        playbackSessionToken: this.config.token,
        watchTimeSeconds: watchTime,
        currentPositionSeconds: position,
        totalDurationSeconds: this.config.duration,
      })
      .pipe(catchError(() => EMPTY))
      .subscribe();
  }

  private clearInterval(): void {
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  ngOnDestroy(): void {
    this.stop();
  }
}
