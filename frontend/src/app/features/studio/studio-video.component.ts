import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardIconComponent } from '@/shared/components/icon/icon.component';
import { ZardProgressBarComponent } from '@/shared/components/progress-bar';
import { VisibilityPipe } from '@/shared/pipes/visibility.pipe';
import { CompactNumberPipe } from '@/shared/pipes/compact-number.pipe';
import type { VideoItem } from '../videos/video.types';

@Component({
  selector: 'studio-video',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe, ZardBadgeComponent, ZardIconComponent, ZardProgressBarComponent, VisibilityPipe, CompactNumberPipe],
  template: `
    <a [routerLink]="['/videos', video().id, 'manage']">
      <div class="flex items-center gap-4 rounded-lg border p-3 hover:bg-accent/50 transition-colors">

        <img
          [src]="video().thumbnails['maxres']?.url"
          alt="Thumbnail"
          class="w-36 shrink-0 rounded-md border object-cover aspect-video"
        />

        <div class="flex flex-1 flex-col gap-1.5 min-w-0">
          <p class="font-medium truncate">{{ video().title }}</p>
          <div class="flex flex-wrap items-center gap-2">
            <z-badge [zType]="visibilityBadgeType()">
              {{ video().visibility | visibilityPipe }}
            </z-badge>
            @if (transcodingLabel()) {
              <z-badge [zType]="transcodingBadgeType()">{{ transcodingLabel() }}</z-badge>
            }
          </div>
          @if (video().processingDetails?.transcodingStatus === 'processing') {
            <z-progress-bar
              [progress]="video().processingDetails!.transcodingProgress"
              class="w-36 mt-1"
            />
          }
        </div>

        <div class="hidden md:flex items-center gap-6 text-sm text-muted-foreground shrink-0">
          <div class="flex items-center gap-1.5">
            <z-icon zType="eye" zSize="sm" />
            <span>{{ video().statistics?.viewCount | compactNumber }}</span>
          </div>
          <div class="flex items-center gap-1.5">
            <z-icon zType="thumbs-up" zSize="sm" />
            <span>{{ video().statistics?.likeCount | compactNumber }}</span>
          </div>
          <div class="flex items-center gap-1.5">
            <z-icon zType="message-square" zSize="sm" />
            <span>{{ video().statistics?.commentCount | compactNumber }}</span>
          </div>
        </div>

        <div class="hidden lg:block text-xs text-muted-foreground shrink-0 w-24 text-right">
          {{ video().publishedAt | date:'mediumDate' }}
        </div>

      </div>
    </a>
  `,
})
export class StudioVideoComponent {
  readonly video = input.required<VideoItem>();

  protected readonly visibilityBadgeType = computed(() => {
    switch (this.video().visibility) {
      case 'PUBLIC': return 'default' as const;
      case 'PRIVATE': return 'secondary' as const;
      case 'NOT_LISTED': return 'outline' as const;
    }
  });

  protected readonly transcodingBadgeType = computed(() => {
    switch (this.video().processingDetails?.transcodingStatus) {
      case 'processing': return 'outline' as const;
      case 'queued': return 'secondary' as const;
      case 'failed': return 'destructive' as const;
      default: return 'default' as const;
    }
  });

  protected readonly transcodingLabel = computed(() => {
    switch (this.video().processingDetails?.transcodingStatus) {
      case 'processing': return 'Processando...';
      case 'queued': return 'Na fila';
      case 'failed': return 'Falhou';
      default: return null;
    }
  });
}
