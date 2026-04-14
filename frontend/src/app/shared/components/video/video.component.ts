import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  PLATFORM_ID,
  signal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isPlatformBrowser } from '@angular/common';
import { switchMap, catchError, of, map } from 'rxjs';

import { AuthStore } from '@/core/stores/auth.store';
import { VodService } from '@/features/vod/vod.service';
import { PlaybackTrackerService } from '@/features/vod/playback-tracker.service';
import { VideoService } from '@/features/videos/video.service';
import { InteractionService } from '@/features/interactions/interaction.service';
import type { Resolution, VideoItem } from '@/features/videos/video.types';
import type { WatchVodResponse } from '@/features/vod/vod.types';

import { SidebarComponent } from '@/shared/components/sidebar/sidebar.component';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardAlertComponent } from '@/shared/components/alert';
import { VideoPlayerSectionComponent } from './video-player-section.component';
import { VideoInfoComponent } from './video-info.component';
import { VideoDescriptionComponent } from './video-description.component';
import { VideoCommentsComponent } from './video-comments.component';
import { VideoRelatedComponent } from './video-related.component';

import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-video',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PlaybackTrackerService],
  imports: [
    SidebarComponent,
    ZardIconComponent,
    ZardAlertComponent,
    VideoPlayerSectionComponent,
    VideoInfoComponent,
    VideoDescriptionComponent,
    VideoCommentsComponent,
    VideoRelatedComponent,
  ],
  template: `
    <div class="min-h-screen md:flex">
      <app-sidebar />

      <main class="w-full px-4 py-6 md:px-8">
        @if (loading()) {
          <div class="flex h-[60vh] items-center justify-center">
            <z-icon zType="loader-circle" class="animate-spin text-muted-foreground" zSize="xl" />
          </div>
        }

        @if (loadError()) {
          <div class="mx-auto max-w-2xl py-12">
            <z-alert
              zType="destructive"
              zTitle="Não foi possível carregar o vídeo"
              zDescription="Verifique sua conexão ou tente novamente mais tarde."
            />
          </div>
        }

        @if (!loading() && !loadError()) {
          <div class="mx-auto flex w-full max-w-7xl flex-col gap-6 lg:flex-row lg:gap-8">
            <div class="flex min-w-0 flex-1 flex-col gap-6">
              <video-player-section
                [currentTime]="this.vod()?.lastWatch?.accumulatedWatchTime ?? 0"
                [manifestUrl]="manifestUrl()"
                [availableResolutions]="availableResolutions()"
                [selectedResolution]="selectedResolution()"
                [resolutionLoading]="resolutionLoading()"
                [resolutionError]="resolutionError()"
                (playing)="onPlay()"
                (paused)="onPause()"
                (ended)="onEnded()"
                (timeUpdate)="onTimeUpdate($event)"
                (streamReady)="onStreamReady()"
                (resolutionChange)="onResolutionChange($event)"
              />

              <video-info
                [video]="video()"
                [likedByMe]="likedByMe()"
                [dislikedByMe]="dislikedByMe()"
                [likeCount]="likeCount()"
                [cloudFrontBase]="cloudFrontBase()"
                (liked)="onLike()"
                (disliked)="onDislike()"
              />

              <div class="h-px bg-border"></div>

              <video-description
                [description]="video()?.description ?? vod()?.videoDetails?.description ?? ''"
              />

              <div class="h-px bg-border"></div>

              <video-comments [videoId]="videoId" [user]="this.authStore.creator()" />
            </div>

            <aside class="w-full shrink-0 space-y-3 lg:w-80 xl:w-96">
              <video-related [currentVideoId]="videoId" />
            </aside>
          </div>
        }
      </main>
    </div>
  `,
})
export class VideoComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly vodService = inject(VodService);
  private readonly videoService = inject(VideoService);
  private readonly interactionService = inject(InteractionService);
  private readonly tracker = inject(PlaybackTrackerService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly platformId = inject(PLATFORM_ID);

  readonly authStore = inject(AuthStore);
  protected readonly cloudFrontBase = signal(`https://${environment.defaultCloudfrontUrl}`);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly vod = signal<WatchVodResponse | null>(null);
  protected readonly video = signal<VideoItem | null>(null);
  protected readonly likedByMe = signal(false);
  protected readonly dislikedByMe = signal(false);
  protected readonly likeCount = signal(0);
  protected readonly selectedResolution = signal<Resolution | null>(null);
  protected readonly resolutionLoading = signal(false);
  protected readonly resolutionError = signal(false);

  protected readonly manifestUrl = computed(() => this.vod()?.manifestUrl ?? null);
  protected readonly availableResolutions = computed<Resolution[]>(() =>
    this.extractResolutions(this.video()?.contentDetails?.resolutions),
  );
  protected readonly userId = computed(() => this.authStore.creator()?.id ?? null);

  protected videoId = '';

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        switchMap((params) => {
          this.videoId = params.get('id') ?? '';

          if (!this.videoId) {
            this.loadError.set(true);
            this.loading.set(false);
            return of(null);
          }

          this.loading.set(true);
          this.loadError.set(false);
          this.resolutionLoading.set(false);
          this.resolutionError.set(false);
          this.selectedResolution.set(null);

          return this.videoService.getVideo(this.videoId, 'contentDetails,statistics,channel').pipe(
            catchError(() => of(null)),
            switchMap((video) => {
              const initialResolution = this.pickInitialResolution(video);
              if (!initialResolution) return of({ vod: null, video });
              this.selectedResolution.set(initialResolution);
              return this.vodService.watchVod(this.videoId, initialResolution).pipe(
                map((vod) => ({ vod, video })),
                catchError(() => of({ vod: null, video })),
              );
            }),
          );
        }),
      )
      .subscribe((result) => {
        if (!result?.vod) {
          this.loadError.set(true);
          this.loading.set(false);
          return;
        }

        this.vod.set(result.vod);
        this.video.set(result.video);
        this.likedByMe.set(result.video?.likedByMe ?? false);
        this.likeCount.set(result.video?.statistics?.likeCount ?? 0);
        this.loading.set(false);

        if (isPlatformBrowser(this.platformId)) {
          this.tracker.start({
            token: result.vod.playbackToken,
            duration: result.vod.videoDetails.duration,
          });
        }
      });
  }

  protected onPlay(): void {
    this.tracker.onPlay();
  }
  protected onPause(): void {
    this.tracker.onPause();
  }
  protected onEnded(): void {
    this.tracker.onEnded();
  }
  protected onTimeUpdate(currentTime: number): void {
    this.tracker.onTimeUpdate(currentTime);
  }
  protected onStreamReady(): void {}

  protected onResolutionChange(resolution: Resolution): void {
    if (!this.videoId || this.selectedResolution() === resolution || this.resolutionLoading())
      return;

    this.resolutionLoading.set(true);
    this.resolutionError.set(false);

    this.vodService
      .watchVod(this.videoId, resolution)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of(null)),
      )
      .subscribe((vod) => {
        this.resolutionLoading.set(false);
        if (!vod) {
          this.resolutionError.set(true);
          return;
        }
        this.selectedResolution.set(resolution);
        this.vod.set(vod);
        if (isPlatformBrowser(this.platformId)) {
          this.tracker.start({ token: vod.playbackToken, duration: vod.videoDetails.duration });
        }
      });
  }

  protected onLike(): void {
    if (!this.videoId) return;
    if (this.likedByMe()) {
      this.interactionService
        .removeReaction(this.videoId, 'like')
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          catchError(() => of(null)),
        )
        .subscribe((res) => {
          if (res !== null) {
            this.likedByMe.set(false);
            this.likeCount.set(res.likesCount);
          }
        });
    } else {
      if (this.dislikedByMe()) this.dislikedByMe.set(false);
      this.interactionService
        .react(this.videoId, 'like')
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          catchError(() => of(null)),
        )
        .subscribe((res) => {
          if (res !== null) {
            this.likedByMe.set(true);
            this.likeCount.set(res.likesCount);
          }
        });
    }
  }

  protected onDislike(): void {
    if (!this.videoId) return;
    if (this.dislikedByMe()) {
      this.interactionService
        .removeReaction(this.videoId, 'dislike')
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          catchError(() => of(null)),
        )
        .subscribe((res) => {
          if (res !== null) this.dislikedByMe.set(false);
        });
    } else {
      if (this.likedByMe()) this.likedByMe.set(false);
      this.interactionService
        .react(this.videoId, 'dislike')
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          catchError(() => of(null)),
        )
        .subscribe((res) => {
          if (res !== null) {
            this.dislikedByMe.set(true);
            this.likeCount.set(res.likesCount);
          }
        });
    }
  }

  private extractResolutions(raw: string | undefined): Resolution[] {
    if (!raw) return [];
    const supported: Resolution[] = ['1080p', '720p', '480p', '360p'];
    return supported.filter((r) => raw.includes(r));
  }

  private pickInitialResolution(video: VideoItem | null): Resolution | null {
    const available = this.extractResolutions(video?.contentDetails?.resolutions);
    return available.includes('720p') ? '720p' : (available[0] ?? '720p');
  }
}
