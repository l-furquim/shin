import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { VideoPlayerService } from '@/features/videos/video-player.service';
import type { DashPlayerHandle } from '@/features/videos/video-player.types';
import { ZardAlertComponent } from '@/shared/components/alert';

@Component({
  selector: 'video-player, [video-player]',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ZardAlertComponent],
  template: `
    <div class="w-full overflow-hidden rounded-xl bg-black">
      <video
        #videoElement
        controls
        class="aspect-video w-full bg-black"
        (play)="playing.emit()"
        (pause)="paused.emit()"
        (ended)="ended.emit()"
        (timeupdate)="onTimeUpdate()"
      ></video>
    </div>

    @if (playerError()) {
      <z-alert
        class="mt-3 block"
        zType="destructive"
        zTitle="Erro no player"
        zDescription="Não foi possível carregar o vídeo. Verifique sua conexão e tente novamente."
      ></z-alert>
    }
  `,
})
export class PlayerComponent {
  readonly manifestUrl = input<string | null>(null);

  readonly timeUpdate = output<number>();
  readonly playing = output<void>();
  readonly paused = output<void>();
  readonly ended = output<void>();
  readonly streamReady = output<void>();
  readonly playerFailed = output<void>();

  protected readonly playerError = signal(false);

  private readonly videoPlayerService = inject(VideoPlayerService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly videoElement = viewChild<ElementRef<HTMLVideoElement>>('videoElement');

  private dashPlayer: DashPlayerHandle | null = null;

  constructor() {
    effect(() => {
      const url = this.manifestUrl();
      if (url) {
        this.loadPlayer(url);
      } else {
        this.resetPlayer();
      }
    });

    this.destroyRef.onDestroy(() => this.resetPlayer());
  }

  onTimeUpdate(): void {
    const el = this.videoElement()?.nativeElement;
    if (el) {
      this.timeUpdate.emit(el.currentTime);
    }
  }

  private async loadPlayer(manifestUrl: string): Promise<void> {
    this.resetPlayer();
    this.playerError.set(false);

    const element = this.videoElement()?.nativeElement;
    if (!element) return;

    try {
      this.dashPlayer = await this.videoPlayerService.attachPlayer({
        videoElement: element,
        manifestUrl,
        withCredentials: true,
        onStreamInitialized: () => this.streamReady.emit(),
        onError: () => {
          this.playerError.set(true);
          this.playerFailed.emit();
        },
        onQualityChanged: () => {},
      });
    } catch {
      this.playerError.set(true);
      this.playerFailed.emit();
    }
  }

  private resetPlayer(): void {
    if (this.dashPlayer) {
      this.dashPlayer.reset();
      this.dashPlayer = null;
    }
  }
}
