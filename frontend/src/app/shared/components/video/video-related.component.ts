import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { catchError, of } from 'rxjs';
import { VideoService } from '@/features/videos/video.service';
import type { VideoItem } from '@/features/videos/video.types';
import { ZardIconComponent } from '@/shared/components/icon';

@Component({
  selector: 'video-related',
  imports: [RouterLink, DecimalPipe, ZardIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h3 class="text-sm font-semibold text-muted-foreground">A seguir</h3>

    @for (video of videos(); track video.id) {
      <a
        [routerLink]="['/videos', video.id]"
        class="flex gap-3 rounded-xl p-2 transition-colors hover:bg-stone-100"
      >
        <div class="relative h-20 w-36 shrink-0 overflow-hidden rounded-lg bg-stone-200">
          @if (video.thumbnails['default']?.url) {
            <img
              [src]="video.thumbnails['default']!.url"
              [alt]="video.title"
              class="h-full w-full object-cover"
            />
          } @else {
            <div class="flex h-full w-full items-center justify-center">
              <z-icon zType="film" zSize="default" class="text-stone-400" />
            </div>
          }
        </div>
        <div class="flex flex-col gap-1 overflow-hidden">
          <p class="line-clamp-2 text-sm font-medium leading-snug">{{ video.title }}</p>
          <p class="text-xs text-muted-foreground">{{ video.channel.name }}</p>
          <p class="text-xs text-muted-foreground">
            {{ video.statistics?.viewCount ?? 0 | number }} visualizações
          </p>
        </div>
      </a>
    }

    @if (loading()) {
      @for (item of skeletonItems; track item) {
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

    @if (error()) {
      <p class="px-2 text-sm text-muted-foreground">Não foi possível carregar recomendações.</p>
    }
  `,
})
export class VideoRelatedComponent implements OnInit {
  private readonly videoService = inject(VideoService);
  private readonly destroyRef = inject(DestroyRef);

  readonly currentVideoId = input.required<string>();

  protected readonly videos = signal<VideoItem[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);

  protected readonly skeletonItems = [1, 2, 3, 4, 5];

  ngOnInit(): void {
    this.videoService
      .listVideos({ fields: 'statistics,thumbnails,channel', limit: 10 })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of(null)),
      )
      .subscribe((res) => {
        this.loading.set(false);
        if (res) {
          this.videos.set(res.items.filter((v) => v.id !== this.currentVideoId()));
          return;
        }
        this.error.set(true);
      });
  }
}
