import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
  viewChild,
  ElementRef,
  DestroyRef,
  Input,
} from '@angular/core';

import { environment } from '../../../../environments/environment';
import type { Resolution } from '@/features/videos/video.types';
import { VideoPlayerService } from '@/features/videos/video-player.service';
import type { DashPlayerHandle } from '@/features/videos/video-player.types';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardInputDirective } from '@/shared/components/input';
import { ZardSelectImports } from '@/shared/components/select';

@Component({
  selector: 'video-player-area, [video-player-area]',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .video-shell {
      box-shadow: inset 0 1px 0 rgb(255 255 255 / 12%);
    }
  `,
  imports: [
    ZardCardComponent,
    ZardButtonComponent,
    ZardInputDirective,
    ZardIconComponent,
    ZardAlertComponent,
    ...ZardSelectImports,
  ],
  template: `
    <z-card
      class="h-full border-stone-200/70 bg-white/70 backdrop-blur-sm"
      zTitle="Player DASH"
      zDescription="Consome manifest.mpd servido no CloudFront"
    >
      <div class="space-y-4">
        <div class="grid gap-3 sm:grid-cols-2">
          <div class="space-y-2 sm:col-span-1">
            <label for="videoId" class="text-sm font-medium">Video ID</label>
            <input id="videoId" z-input type="text" placeholder="UUID do video" />
          </div>

          <div class="space-y-2 sm:col-span-1">
            <label class="text-sm font-medium">Resolucao inicial</label>
            <z-select [zValue]="resolution()" (zSelectionChange)="onResolutionChange($event)">
              <z-select-item zValue="1080p">1080p</z-select-item>
              <z-select-item zValue="720p">720p</z-select-item>
              <z-select-item zValue="480p">480p</z-select-item>
              <z-select-item zValue="360p">360p</z-select-item>
            </z-select>
          </div>
        </div>

        <div class="video-shell overflow-hidden rounded-xl border bg-black">
          <video
            #videoElement
            controls
            class="h-64 w-full bg-black md:h-72"
            (timeupdate)="onTimeUpdate()"
          ></video>
        </div>

        @if (playerError()) {
          <z-alert
            zType="destructive"
            zTitle="Erro no player"
            [zDescription]="playerStatus()"
          ></z-alert>
        } @else {
          <z-alert
            zTitle="Status do player"
            [zDescription]="playerStatus()"
            zIcon="activity"
          ></z-alert>
        }

        <div class="grid grid-cols-2 gap-3">
          <div class="rounded-lg border p-3">
            <p class="text-muted-foreground text-xs">Qualidade renderizada</p>
            <p class="text-sm font-medium">{{ qualityLabel() }}</p>
          </div>
          <div class="rounded-lg border p-3">
            <p class="text-muted-foreground text-xs">Buffer atual</p>
            <p class="text-sm font-medium">{{ bufferLabel() }}</p>
          </div>
        </div>

        <div class="rounded-lg border border-dashed p-3">
          <p class="text-muted-foreground text-xs">Manifest URL</p>
          <p class="truncate text-sm">{{ manifestUrl() || '-' }}</p>
        </div>
      </div>

      <div card-footer class="w-full items-start gap-3">
        <z-button [zDisabled]="!canLoadPlayer()" zType="default" (click)="loadPlayer()">
          <z-icon zType="arrow-right" />
          Carregar stream
        </z-button>
      </div>
    </z-card>
  `,
})
export class PlayerComponent {
  @Input() protected videoId!: string;

  private readonly destroyRef = inject(DestroyRef);
  private readonly videoPlayerService = inject(VideoPlayerService);

  private dashPlayer: DashPlayerHandle | null = null;

  protected readonly cloudfrontUrl = signal(environment.defaultCloudfrontUrl);
  protected readonly resolution = signal<Resolution>('720p');
  protected readonly playerStatus = signal('Informe CDN e Video ID para carregar o player.');
  protected readonly playerError = signal(false);
  protected readonly qualityLabel = signal('-');
  protected readonly bufferLabel = signal('-');

  protected readonly videoElement = viewChild<ElementRef<HTMLVideoElement>>('videoElement');

  protected readonly manifestUrl = computed(() =>
    this.videoPlayerService.buildManifestUrl({
      cdnUrl: this.cloudfrontUrl(),
      videoId: this.videoId,
      resolution: this.resolution(),
    }),
  );

  protected readonly canLoadPlayer = computed(() => !!this.manifestUrl());

  constructor() {
    this.destroyRef.onDestroy(() => this.destroyPlayer());
  }

  onResolutionChange(value: string | string[]): void {
    if (typeof value === 'string' && this.videoPlayerService.isResolution(value)) {
      this.resolution.set(value);
    }
  }

  async loadPlayer(): Promise<void> {
    const element = this.videoElement()?.nativeElement;
    const manifest = this.manifestUrl();

    if (!element || !manifest) {
      this.playerError.set(true);
      this.playerStatus.set('Preencha CloudFront URL e Video ID para carregar o player.');
      return;
    }

    this.playerError.set(false);
    this.playerStatus.set('Carregando stream DASH...');

    try {
      this.destroyPlayer();

      this.dashPlayer = await this.videoPlayerService.attachPlayer({
        videoElement: element,
        manifestUrl: manifest,
        onStreamInitialized: () => {
          this.playerError.set(false);
          this.playerStatus.set('Video carregado com sucesso.');
        },
        onError: () => {
          this.playerError.set(true);
          this.playerStatus.set('Falha ao carregar stream. Verifique URL, Video ID e manifesto.');
        },
        onQualityChanged: (qualityLabel) => {
          this.qualityLabel.set(qualityLabel);
        },
      });
    } catch (error) {
      this.playerError.set(true);
      const message =
        error instanceof Error
          ? error.message
          : 'Nao foi possivel iniciar o dash.js neste ambiente.';
      this.playerStatus.set(message);
    }
  }

  onTimeUpdate(): void {
    const video = this.videoElement()?.nativeElement;
    if (!video) {
      return;
    }

    this.bufferLabel.set(this.videoPlayerService.getBufferLevel(video));
  }

  private destroyPlayer(): void {
    if (this.dashPlayer) {
      this.dashPlayer.reset();
      this.dashPlayer = null;
    }
  }
}
