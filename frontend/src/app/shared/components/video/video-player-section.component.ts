import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { PlayerComponent } from '@/shared/components/player/player.component';
import { ZardIconComponent } from '@/shared/components/icon';
import type { Resolution } from '@/features/videos/video.types';

@Component({
  selector: 'video-player-section',
  imports: [PlayerComponent, ZardIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <video-player
      [manifestUrl]="manifestUrl()"
      (playing)="playing.emit()"
      (paused)="paused.emit()"
      (ended)="ended.emit()"
      (timeUpdate)="timeUpdate.emit($event)"
      (streamReady)="streamReady.emit()"
    />

    @if (availableResolutions().length > 0) {
      <div class="flex flex-wrap items-center gap-2 mt-3">
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
            (click)="resolutionChange.emit(resolution)"
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
      <p class="mt-2 text-sm text-destructive">
        Não foi possível trocar a resolução agora. Tente novamente.
      </p>
    }
  `,
})
export class VideoPlayerSectionComponent {
  readonly manifestUrl = input<string | null>(null);
  readonly availableResolutions = input<Resolution[]>([]);
  readonly selectedResolution = input<Resolution | null>(null);
  readonly resolutionLoading = input(false);
  readonly resolutionError = input(false);

  readonly playing = output<void>();
  readonly paused = output<void>();
  readonly ended = output<void>();
  readonly timeUpdate = output<number>();
  readonly streamReady = output<void>();
  readonly resolutionChange = output<Resolution>();
}
