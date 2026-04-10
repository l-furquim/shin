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
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, DatePipe, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { switchMap, catchError, of, map } from 'rxjs';

import { AuthStore } from '@/core/stores/auth.store';
import { VodService } from '@/features/vod/vod.service';
import { PlaybackTrackerService } from '@/features/vod/playback-tracker.service';
import { VideoService } from '@/features/videos/video.service';
import { CommentService } from '@/features/comments/comment.service';
import { InteractionService } from '@/features/interactions/interaction.service';
import type { Resolution, VideoItem } from '@/features/videos/video.types';
import type { WatchVodResponse } from '@/features/vod/vod.types';
import type { CommentDto } from '@/features/comments/comment.types';

import { PlayerComponent } from '../player/player.component';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { ZardButtonComponent } from '../button';
import { ZardIconComponent } from '../icon';
import { ZardAlertComponent } from '../alert';

import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-video',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PlaybackTrackerService],
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    RouterLink,
    PlayerComponent,
    SidebarComponent,
    ZardButtonComponent,
    ZardIconComponent,
    ZardAlertComponent,
  ],
  template: `
    <div class="min-h-screen md:flex">
      <app-sidebar></app-sidebar>

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
            ></z-alert>
          </div>
        }

        @if (!loading() && !loadError()) {
          <div class="mx-auto flex w-full max-w-7xl flex-col gap-6 lg:flex-row lg:gap-8">
            <div class="flex min-w-0 flex-1 flex-col gap-6">
              <video-player
                [manifestUrl]="manifestUrl()"
                (playing)="onPlay()"
                (paused)="onPause()"
                (ended)="onEnded()"
                (timeUpdate)="onTimeUpdate($event)"
                (streamReady)="onStreamReady()"
              ></video-player>

              @if (availableResolutions().length > 0) {
                <div class="flex flex-wrap items-center gap-2">
                  <span class="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Qualidade
                  </span>
                  @for (resolution of availableResolutions(); track resolution) {
                    <button
                      type="button"
                      class="rounded-full border px-3 py-1 text-xs font-medium transition-colors"
                      [class.bg-stone-900]="selectedResolution() === resolution"
                      [class.text-white]="selectedResolution() === resolution"
                      [class.border-stone-900]="selectedResolution() === resolution"
                      [class.hover:bg-stone-100]="selectedResolution() !== resolution"
                      [disabled]="resolutionLoading() || selectedResolution() === resolution"
                      (click)="onResolutionChange(resolution)"
                    >
                      {{ resolution }}
                    </button>
                  }
                  @if (resolutionLoading()) {
                    <z-icon zType="loader-circle" class="animate-spin text-muted-foreground" zSize="sm" />
                  }
                </div>
              }

              @if (resolutionError()) {
                <p class="text-sm text-destructive">
                  Não foi possível trocar a resolução agora. Tente novamente.
                </p>
              }

              <div class="space-y-2">
                <h1 class="text-xl font-bold leading-tight md:text-2xl">
                  {{ video()?.title ?? vod()?.videoDetails?.title }}
                </h1>
                @if (!video()) {
                  <p class="text-xs text-muted-foreground">
                    Nao foi possivel carregar todos os detalhes do video.
                  </p>
                }
                <p class="text-muted-foreground text-sm">
                  {{ video()?.statistics?.viewCount ?? 0 | number }} visualizações
                  @if (video()?.publishedAt) {
                    · {{ video()!.publishedAt | date: 'dd/MM/yyyy' }}
                  }
                </p>
              </div>

              <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div class="flex items-center gap-3">
                  @if (video()?.channel?.avatarUrl) {
                    <img
                      [src]="cloudFrontBase() + video()?.channel?.avatarUrl"
                      [alt]="video()!.channel.name"
                      class="h-10 w-10 rounded-full object-cover"
                    />
                  } @else {
                    <div
                      class="flex h-10 w-10 items-center justify-center rounded-full bg-stone-200"
                    >
                      <z-icon zType="user" zSize="sm" class="text-stone-500" />
                    </div>
                  }
                  <p class="font-semibold leading-none">{{ video()?.channel?.name ?? '—' }}</p>
                </div>

                <div class="flex items-center gap-2">
                  <div class="flex overflow-hidden rounded-full border">
                    <button
                      class="flex items-center gap-2 px-4 py-2 text-sm transition-colors hover:bg-stone-100"
                      [class.bg-stone-100]="likedByMe()"
                      (click)="onLike()"
                    >
                      <z-icon zType="thumbs-up" zSize="sm" />
                      <span>{{ likeCount() | number }}</span>
                    </button>
                    <div class="w-px bg-border"></div>
                    <button
                      class="flex items-center gap-2 px-4 py-2 text-sm transition-colors hover:bg-stone-100"
                      [class.bg-stone-100]="dislikedByMe()"
                      (click)="onDislike()"
                    >
                      <z-icon zType="thumbs-down" zSize="sm" />
                    </button>
                  </div>

                  <button
                    class="flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition-colors hover:bg-stone-100"
                  >
                    <z-icon zType="share" zSize="sm" />
                    <span>Compartilhar</span>
                  </button>
                </div>
              </div>

              <div class="h-px bg-border"></div>

              <div class="rounded-xl bg-stone-100 p-4 text-sm leading-relaxed">
                <p class="whitespace-pre-wrap" [class.line-clamp-3]="!descriptionExpanded()">
                  {{ video()?.description ?? vod()?.videoDetails?.description ?? '' }}
                </p>
                @if (!descriptionExpanded()) {
                  <button
                    class="mt-2 font-semibold hover:underline"
                    (click)="descriptionExpanded.set(true)"
                  >
                    Ver mais
                  </button>
                } @else {
                  <button
                    class="mt-2 font-semibold hover:underline"
                    (click)="descriptionExpanded.set(false)"
                  >
                    Ver menos
                  </button>
                }
              </div>

              <div class="h-px bg-border"></div>

              <section class="space-y-5">
                <h2 class="text-base font-semibold">
                  {{ video()?.statistics?.commentCount ?? comments().length }} comentários
                </h2>

                @if (userId()) {
                  <div class="flex gap-3">
                    <div
                      class="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-stone-200"
                    >
                      <z-icon zType="user" zSize="sm" class="text-stone-500" />
                    </div>
                    <div class="flex flex-1 flex-col gap-2">
                      <textarea
                        [(ngModel)]="newCommentText"
                        rows="2"
                        placeholder="Adicione um comentário..."
                        class="w-full resize-none rounded-lg border bg-transparent px-3 py-2 text-sm outline-none focus:border-stone-400 focus:ring-0"
                      ></textarea>
                      <div class="flex justify-end gap-2">
                        <button
                          class="rounded-full px-4 py-1.5 text-sm font-medium hover:bg-stone-100"
                          (click)="newCommentText = ''"
                        >
                          Cancelar
                        </button>
                        <z-button
                          zType="default"
                          [zDisabled]="!newCommentText.trim() || commentSubmitting()"
                          (click)="submitComment()"
                        >
                          Comentar
                        </z-button>
                      </div>
                    </div>
                  </div>
                }

                @for (comment of comments(); track comment.id) {
                  <div class="flex gap-3">
                    <div
                      class="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-stone-200"
                    >
                      <z-icon zType="user" zSize="sm" class="text-stone-500" />
                    </div>
                    <div class="flex-1 space-y-1">
                      <div class="flex items-center gap-2">
                        <span class="text-sm font-semibold">{{ comment.authorId }}</span>
                        <span class="text-muted-foreground text-xs">
                          {{ comment.createdAt | date: 'dd/MM/y' }}
                        </span>
                      </div>
                      <p class="text-sm leading-relaxed">{{ comment.textDisplay }}</p>
                      <div class="flex items-center gap-3 text-xs text-muted-foreground">
                        <button class="flex items-center gap-1 hover:text-foreground">
                          <z-icon zType="thumbs-up" zSize="sm" />
                          {{ comment.likeCount }}
                        </button>
                        <button class="hover:text-foreground">Responder</button>
                        @if (comment.authorId === userId()) {
                          <button
                            class="hover:text-destructive"
                            (click)="deleteComment(comment.id)"
                          >
                            Excluir
                          </button>
                        }
                      </div>
                    </div>
                  </div>
                }

                @if (commentsLoading()) {
                  <div class="flex justify-center py-4">
                    <z-icon zType="loader-circle" class="animate-spin text-muted-foreground" />
                  </div>
                }

                @if (!commentsLoading() && comments().length === 0) {
                  <p class="py-4 text-center text-sm text-muted-foreground">
                    Seja o primeiro a comentar.
                  </p>
                }

                @if (commentsError()) {
                  <p class="py-2 text-sm text-muted-foreground">
                    Nao foi possivel carregar comentarios.
                  </p>
                }
              </section>
            </div>

            <aside class="w-full shrink-0 space-y-3 lg:w-80 xl:w-96">
              <h3 class="text-sm font-semibold text-muted-foreground">A seguir</h3>
              @for (related of relatedVideos(); track related.id) {
                <a
                  [routerLink]="['/videos', related.id]"
                  class="flex gap-3 rounded-xl p-2 transition-colors hover:bg-stone-100"
                >
                  <div class="relative h-20 w-36 shrink-0 overflow-hidden rounded-lg bg-stone-200">
                    @if (related.thumbnails?.get?.('default')?.url) {
                      <img
                        [src]="related.thumbnails.get('default')!.url"
                        [alt]="related.title"
                        class="h-full w-full object-cover"
                      />
                    } @else {
                      <div class="flex h-full w-full items-center justify-center">
                        <z-icon zType="film" zSize="default" class="text-stone-400" />
                      </div>
                    }
                  </div>
                  <div class="flex flex-col gap-1 overflow-hidden">
                    <p class="line-clamp-2 text-sm font-medium leading-snug">{{ related.title }}</p>
                    <p class="text-xs text-muted-foreground">{{ related.channel.name }}</p>
                    <p class="text-xs text-muted-foreground">
                      {{ related.statistics?.viewCount ?? 0 | number }} visualizações
                    </p>
                  </div>
                </a>
              }

              @if (relatedLoading()) {
                @for (item of [1, 2, 3, 4, 5]; track item) {
                  <div class="flex gap-3 rounded-xl p-2">
                    <div class="h-20 w-36 shrink-0 animate-pulse rounded-lg bg-stone-200"></div>
                    <div class="flex flex-1 flex-col gap-2 py-1">
                      <div class="h-3 w-full animate-pulse rounded bg-stone-200"></div>
                      <div class="h-3 w-2/3 animate-pulse rounded bg-stone-200"></div>
                      <div class="h-3 w-1/2 animate-pulse rounded bg-stone-200"></div>
                    </div>
                  </div>
                }
              }

              @if (relatedError()) {
                <p class="px-2 text-sm text-muted-foreground">
                  Nao foi possivel carregar recomendacoes.
                </p>
              }
            </aside>
          </div>
        }
      </main>
    </div>
  `,
})
export class VideoComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authStore = inject(AuthStore);
  private readonly vodService = inject(VodService);
  private readonly videoService = inject(VideoService);
  private readonly commentService = inject(CommentService);
  private readonly interactionService = inject(InteractionService);
  private readonly tracker = inject(PlaybackTrackerService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly platformId = inject(PLATFORM_ID);
  protected readonly cloudFrontBase = signal(`https://${environment.defaultCloudfrontUrl}`);

  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly vod = signal<WatchVodResponse | null>(null);
  protected readonly video = signal<VideoItem | null>(null);
  protected readonly comments = signal<CommentDto[]>([]);
  protected readonly commentsLoading = signal(false);
  protected readonly commentsError = signal(false);
  protected readonly relatedVideos = signal<VideoItem[]>([]);
  protected readonly relatedLoading = signal(true);
  protected readonly relatedError = signal(false);
  protected readonly descriptionExpanded = signal(false);
  protected readonly commentSubmitting = signal(false);
  protected readonly likedByMe = signal(false);
  protected readonly dislikedByMe = signal(false);
  protected readonly likeCount = signal(0);
  protected readonly selectedResolution = signal<Resolution | null>(null);
  protected readonly resolutionLoading = signal(false);
  protected readonly resolutionError = signal(false);

  protected newCommentText = '';

  protected readonly manifestUrl = computed(() => this.vod()?.manifestUrl ?? null);
  protected readonly availableResolutions = computed<Resolution[]>(() => {
    const raw = this.video()?.contentDetails?.resolutions;
    return this.extractResolutions(raw);
  });
  protected readonly userId = computed(() => this.authStore.creator()?.id ?? null);

  private videoId = '';

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

              if (!initialResolution) {
                return of({ vod: null, video });
              }

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
        this.resolutionLoading.set(false);
        this.resolutionError.set(false);
        this.loading.set(false);

        if (isPlatformBrowser(this.platformId)) {
          this.tracker.start({
            token: result.vod.playbackToken,
            duration: result.vod.videoDetails.duration,
          });
        }

        this.loadComments();
        this.loadRelatedVideos();
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
    if (!this.videoId || this.selectedResolution() === resolution || this.resolutionLoading()) return;

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
          this.tracker.start({
            token: vod.playbackToken,
            duration: vod.videoDetails.duration,
          });
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
      if (this.likedByMe()) {
        this.likedByMe.set(false);
      }
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

  protected submitComment(): void {
    const text = this.newCommentText.trim();
    const channelId = this.video()?.channel?.id;

    if (!text || !channelId) return;

    this.commentSubmitting.set(true);
    this.commentService
      .createComment({ videoId: this.videoId, channelId, content: text })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of(null)),
      )
      .subscribe((comment) => {
        this.commentSubmitting.set(false);
        if (comment) {
          this.newCommentText = '';
          this.comments.update((list) => [comment, ...list]);
        }
      });
  }

  protected deleteComment(commentId: string): void {
    this.commentService
      .deleteComment(commentId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of(null)),
      )
      .subscribe(() => {
        this.comments.update((list) => list.filter((c) => c.id !== commentId));
      });
  }

  private loadComments(): void {
    this.commentsLoading.set(true);
    this.commentsError.set(false);
    this.commentService
      .listThreads(this.videoId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of(null)),
      )
      .subscribe((threads) => {
        if (!threads) {
          this.commentsError.set(true);
          this.commentsLoading.set(false);
          return;
        }

        if (!threads.items.length) {
          this.commentsLoading.set(false);
          return;
        }

        const ids = threads.items.map((t) => t.id);
        this.commentService
          .listComments({ ids, maxResults: ids.length })
          .pipe(
            takeUntilDestroyed(this.destroyRef),
            catchError(() => of(null)),
          )
          .subscribe((res) => {
            this.commentsLoading.set(false);
            if (res) {
              this.comments.set(res.items);
              return;
            }

            this.commentsError.set(true);
          });
      });
  }

  private loadRelatedVideos(): void {
    this.relatedLoading.set(true);
    this.relatedError.set(false);
    this.videoService
      .listVideos({ fields: 'statistics,thumbnails,channel,tags,contentDetails', limit: 10 })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of(null)),
      )
      .subscribe((res) => {
        this.relatedLoading.set(false);
        if (res) {
          this.relatedVideos.set(res.items.filter((v) => v.id !== this.videoId));
          return;
        }

        this.relatedError.set(true);
      });
  }

  private extractResolutions(raw: string | undefined): Resolution[] {
    if (!raw) return [];

    const supported: Resolution[] = ['1080p', '720p', '480p', '360p'];
    return supported.filter((resolution) => raw.includes(resolution));
  }

  private pickInitialResolution(video: VideoItem | null): Resolution | null {
    const available = this.extractResolutions(video?.contentDetails?.resolutions);

    if (available.includes('720p')) {
      return '720p';
    }

    return available[0] ?? '720p';
  }
}
