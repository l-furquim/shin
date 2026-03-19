import { Resolution } from '@/features/videos/video.types';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { firstValueFrom, lastValueFrom, tap } from 'rxjs';
import { ZardCardComponent } from '../card';
import { ZardButtonComponent } from '../button';
import { ZardSelectComponent, ZardSelectItemComponent } from '../select';
import { ZardAlertComponent } from '../alert';
import { ZardIconComponent } from '../icon';
import { VideoService } from '@/features/videos/video.service';
import { CreatorStore } from '@/core/stores/creator.store';
import { UploadService } from '@/features/uploads/upload.service';

@Component({
  imports: [
    ZardCardComponent,
    ZardButtonComponent,
    ZardSelectComponent,
    ZardSelectItemComponent,
    ZardAlertComponent,
    ZardIconComponent,
  ],
  selector: 'upload-area, [upload-area]',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .upload-progress {
      transition: width 0.2s ease;
    }
  `,
  template: `
    <z-card
      class="h-full border-stone-200/70 bg-white/70 backdrop-blur-sm"
      zTitle="Upload de Video"
      zDescription="Envia arquivo em partes para /api/v1/uploads/video/chunk"
    >
      <div class="space-y-4">
        <div class="space-y-2">
          <label for="fileInput" class="text-sm font-medium">Arquivo de video</label>
          <input id="fileInput" type="file" accept="video/*" (change)="onFileChange($event)" />
        </div>

        <div class="rounded-lg border border-dashed p-3">
          <p class="text-sm font-medium">Arquivo selecionado</p>
          <p class="text-muted-foreground mt-1 text-xs">{{ fileName() }}</p>
          <p class="text-muted-foreground text-xs">{{ fileSize() }}</p>
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium">Resolucoes desejadas</label>
          <z-select
            [zValue]="uploadResolutions()"
            [zMultiple]="true"
            zPlaceholder="Selecione resolucoes"
            (zSelectionChange)="onUploadResolutionsChange($event)"
          >
            <z-select-item zValue="1080p">1080p</z-select-item>
            <z-select-item zValue="720p">720p</z-select-item>
            <z-select-item zValue="480p">480p</z-select-item>
            <z-select-item zValue="360p">360p</z-select-item>
          </z-select>
        </div>

        <div class="h-2 w-full overflow-hidden rounded-full bg-muted">
          <div
            class="upload-progress h-full rounded-full bg-primary"
            [style.width.%]="uploadProgress()"
          ></div>
        </div>
        <div class="flex items-center justify-between text-xs">
          <span class="text-muted-foreground">Progresso</span>
          <span class="font-medium">{{ uploadProgress() }}%</span>
        </div>

        @if (uploadError()) {
          <z-alert
            zType="destructive"
            zTitle="Falha no upload"
            [zDescription]="uploadStatus()"
          ></z-alert>
        } @else {
          <z-alert zTitle="Status" [zDescription]="uploadStatus()" zIcon="info"></z-alert>
        }
      </div>

      <div card-footer class="w-full items-start gap-3">
        <z-button
          [zDisabled]="!canUpload()"
          [zLoading]="isUploading()"
          zType="default"
          (click)="onUpload()"
        >
          <z-icon zType="arrow-up-right" />
          {{ isUploading() ? 'Enviando...' : 'Enviar video' }}
        </z-button>

        @if (uploadInfoVideoId()) {
          <div class="text-muted-foreground text-xs">
            <p>
              <span class="font-medium text-foreground">Video ID:</span> {{ uploadInfoVideoId() }}
            </p>
            <p>
              <span class="font-medium text-foreground">Status:</span> {{ uploadInfoVideoStatus() }}
            </p>
          </div>
        }
      </div>
    </z-card>
  `,
})
export class UploadComponent {
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly uploadProgress = signal(0);
  protected readonly uploadStatus = signal('Selecione um video e clique em enviar.');
  protected readonly uploadError = signal(false);
  protected readonly uploadInfoVideoId = signal<string | null>(null);
  protected readonly uploadInfoVideoStatus = signal<string | null>(null);
  protected readonly isUploading = signal(false);
  protected readonly uploadResolutions = signal<Resolution[]>(['1080p', '720p', '480p']);

  protected readonly canUpload = computed(() => !this.isUploading() && !!this.selectedFile());

  protected readonly fileSize = computed(() => this.formatBytes(this.selectedFile()?.size ?? 0));
  protected readonly fileName = computed(
    () => this.selectedFile()?.name ?? 'Nenhum arquivo selecionado',
  );

  private readonly videoService = inject(VideoService);
  private readonly uploadService = inject(UploadService);
  private readonly store = inject(CreatorStore);
  private readonly user = computed(() => this.store.$creator());

  async onUpload(): Promise<void> {
    const file = this.selectedFile();
    if (!file || this.isUploading()) {
      return;
    }

    this.isUploading.set(true);
    this.uploadError.set(false);
    this.uploadProgress.set(0);

    try {
      this.uploadStatus.set('Criando entrada de video...');

      const { id } = this.user() ?? {};

      if (!id) {
        this.uploadError.set(true);
        this.uploadStatus.set('Usuario nao autenticado para upload.');
        return;
      }

      const initVideo = await firstValueFrom(this.videoService.initVideo(id));

      this.uploadInfoVideoId.set(initVideo.videoId);
      this.uploadInfoVideoStatus.set(initVideo.status);

      this.uploadStatus.set('Iniciando upload em chunks...');
      const uploadData = await firstValueFrom(
        this.uploadService.initiateChunkedUpload({
          videoId: initVideo.videoId,
          fileName: file.name,
          fileSize: file.size,
          contentType: file.type,
          resolutions: this.uploadResolutions(),
          userId: id,
        }),
      );

      await lastValueFrom(
        this.uploadService
          .uploadChunks({
            file,
            uploadId: uploadData.uploadId,
            totalChunks: uploadData.totalChunks,
          })
          .pipe(
            tap(({ progress, chunkNumber, totalChunks }) => {
              this.uploadProgress.set(progress);
              this.uploadStatus.set(`Enviando chunk ${chunkNumber}/${totalChunks}...`);
            }),
          ),
      );

      this.uploadStatus.set('Upload finalizado. O video foi enviado para processamento.');
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Erro inesperado no upload';
      this.uploadError.set(true);
      this.uploadStatus.set(errorMessage);
    } finally {
      this.isUploading.set(false);
    }
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const [file] = input?.files ?? [];

    this.selectedFile.set(file ?? null);
    this.uploadProgress.set(0);

    if (!file) {
      this.uploadStatus.set('Nenhum arquivo selecionado.');
      return;
    }

    this.uploadError.set(false);
    this.uploadStatus.set(`Arquivo pronto: ${file.name}`);
  }

  onUploadResolutionsChange(value: string | string[]): void {
    if (!Array.isArray(value) || value.length === 0) {
      return;
    }

    const values = value.filter(this.isResolution) as Resolution[];
    if (values.length > 0) {
      this.uploadResolutions.set(values);
    }
  }

  private formatBytes(bytes: number): string {
    if (bytes === 0) {
      return '0 Bytes';
    }

    const unit = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const index = Math.floor(Math.log(bytes) / Math.log(unit));
    const value = bytes / unit ** index;
    return `${value.toFixed(value < 10 && index > 0 ? 2 : 0)} ${sizes[index]}`;
  }

  private isResolution(value: string): value is Resolution {
    return value === '360p' || value === '480p' || value === '720p' || value === '1080p';
  }
}
