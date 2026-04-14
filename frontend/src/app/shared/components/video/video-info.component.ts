import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ZardIconComponent } from '@/shared/components/icon';
import type { VideoItem } from '@/features/videos/video.types';

@Component({
  selector: 'video-info',
  imports: [ZardIconComponent, DatePipe, DecimalPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-2">
      <h1 class="text-xl font-bold leading-tight md:text-2xl">
        {{ video()?.title }}
      </h1>
      <p class="text-muted-foreground text-sm">
        {{ video()?.statistics?.viewCount ?? 0 | number }} visualizações
        @if (video()?.publishedAt) {
          · {{ video()!.publishedAt | date: 'dd/MM/yyyy' }}
        }
      </p>
    </div>

    <div class="flex flex-col gap-4 pt-5 sm:flex-row sm:items-center sm:justify-between">
      <div class="flex items-center gap-3">
        @if (video()?.channel?.avatarUrl) {
          <img [src]="video()!.channel.avatarUrl" class="h-10 w-10 rounded-full object-cover" />
        } @else {
          <div class="flex h-10 w-10 items-center justify-center rounded-full bg-stone-200">
            <z-icon zType="user" zSize="sm" class="text-stone-500" />
          </div>
        }
        <p class="font-semibold leading-none">{{ video()?.channel?.name ?? '—' }}</p>
      </div>

      <div class="flex items-center gap-2">
        <div class="flex overflow-hidden rounded-full border">
          <button
            type="button"
            class="flex items-center gap-2 px-4 py-2 text-sm transition-colors hover:bg-stone-100"
            [class.bg-stone-100]="likedByMe()"
            (click)="liked.emit()"
          >
            <z-icon zType="thumbs-up" zSize="sm" />
            <span>{{ likeCount() | number }}</span>
          </button>
          <div class="w-px bg-border"></div>
          <button
            type="button"
            class="flex items-center gap-2 px-4 py-2 text-sm transition-colors hover:bg-stone-100"
            [class.bg-stone-100]="dislikedByMe()"
            (click)="disliked.emit()"
          >
            <z-icon zType="thumbs-down" zSize="sm" />
          </button>
        </div>

        <button
          type="button"
          class="flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition-colors hover:bg-stone-100"
        >
          <z-icon zType="share" zSize="sm" />
          <span>Compartilhar</span>
        </button>
      </div>
    </div>
  `,
})
export class VideoInfoComponent {
  readonly video = input<VideoItem | null>(null);
  readonly likedByMe = input(false);
  readonly dislikedByMe = input(false);
  readonly likeCount = input(0);
  readonly cloudFrontBase = input('');

  readonly liked = output<void>();
  readonly disliked = output<void>();
}
