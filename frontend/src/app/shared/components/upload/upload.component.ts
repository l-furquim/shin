import { Resolution } from '@/features/videos/video.types';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { lastValueFrom, tap } from 'rxjs';
import { ZardButtonComponent } from '../button';
import { ZardSelectComponent, ZardSelectItemComponent } from '../select';
import { ZardAlertComponent } from '../alert';
import { ZardIconComponent } from '../icon';
import { AuthStore } from '@/core/stores/auth.store';
import { UploadService } from '@/features/uploads/upload.service';
import { DragDropDirective } from '@/shared/directives/drag-drop.directive';

type UploadState = 'idle' | 'file-selected' | 'uploading' | 'success' | 'error';

const VIDEO_MIME_TYPES = new Set([
  'video/mp4',
  'video/mkv',
  'video/x-matroska',
  'video/quicktime',
  'video/avi',
  'video/x-msvideo',
  'video/webm',
  'video/ogg',
]);

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 * 1024; // 10 GB

@Component({
  imports: [
    DragDropDirective,
    ZardButtonComponent,
    ZardSelectComponent,
    ZardSelectItemComponent,
    ZardAlertComponent,
    ZardIconComponent,
  ],
  selector: 'upload-area, [upload-area]',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .upload-progress { transition: width 0.3s ease; }
    .drop-zone { transition: border-color 0.15s ease, background-color 0.15s ease; }
  `,
  template: `
    <div class="w-full space-y-5">

      <!-- Drop zone -->
      <div
        appDragDrop
        #dnd="dragDrop"
        (fileDropped)="onFilesDropped($event)"
        class="drop-zone relative rounded-2xl border-2 border-dashed cursor-pointer select-none"
        [class]="dropZoneClasses(dnd.isDragging())"
        (click)="fileInput.click()"
      >
        <input
          #fileInput
          type="file"
          accept="video/*"
          class="hidden"
          (change)="onFileInputChange($event)"
        />

        @switch (state()) {
          @case ('idle') {
            <div class="flex flex-col items-center gap-4 py-14 px-6 pointer-events-none text-center">
              <div
                class="rounded-full p-4 transition-colors duration-150"
                [class]="dnd.isDragging() ? 'bg-primary/20' : 'bg-muted'"
              >
                <z-icon zType="cloud-upload" zSize="lg" [class]="dnd.isDragging() ? 'text-primary' : 'text-muted-foreground'" />
              </div>
              <div class="space-y-1">
                <p class="font-semibold text-foreground">
                  {{ dnd.isDragging() ? 'Solte o arquivo aqui' : 'Arraste seu vídeo aqui' }}
                </p>
                <p class="text-sm text-muted-foreground">ou clique para selecionar</p>
              </div>
              <p class="text-xs text-muted-foreground/70 border border-border/50 rounded-full px-3 py-1">
                MP4 · MKV · MOV · AVI · WebM — até 10 GB
              </p>
            </div>
          }

          @case ('file-selected') {
            <div class="flex items-center gap-4 p-5 pointer-events-none">
              <div class="rounded-xl bg-primary/10 p-3 shrink-0">
                <z-icon zType="film" zSize="default" class="text-primary" />
              </div>
              <div class="flex-1 min-w-0 space-y-0.5">
                <p class="font-medium text-sm truncate">{{ fileName() }}</p>
                <p class="text-xs text-muted-foreground">{{ fileSize() }}</p>
              </div>
              <button
                type="button"
                class="pointer-events-auto shrink-0 rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
                (click)="clearFile($event)"
                aria-label="Remover arquivo"
              >
                <z-icon zType="x" zSize="sm" />
              </button>
            </div>
          }

          @case ('uploading') {
            <div class="p-5 space-y-4 pointer-events-none">
              <div class="flex items-center gap-3">
                <div class="rounded-xl bg-primary/10 p-3 shrink-0">
                  <z-icon zType="film" zSize="default" class="text-primary" />
                </div>
                <div class="flex-1 min-w-0">
                  <p class="font-medium text-sm truncate">{{ fileName() }}</p>
                  <p class="text-xs text-muted-foreground">{{ fileSize() }}</p>
                </div>
              </div>

              <div class="space-y-2">
                <div class="flex items-center justify-between text-xs">
                  <div class="flex items-center gap-1.5 text-muted-foreground">
                    <z-icon zType="loader-circle" zSize="sm" class="animate-spin" />
                    <span>{{ uploadStatus() }}</span>
                  </div>
                  <span class="font-mono font-medium">{{ uploadProgress() }}%</span>
                </div>
                <div class="h-1.5 w-full overflow-hidden rounded-full bg-muted">
                  <div
                    class="upload-progress h-full rounded-full bg-primary"
                    [style.width.%]="uploadProgress()"
                  ></div>
                </div>
              </div>
            </div>
          }

          @case ('success') {
            <div class="flex items-center gap-4 p-5 pointer-events-none">
              <div class="rounded-xl bg-emerald-500/10 p-3 shrink-0">
                <z-icon zType="circle-check" zSize="default" class="text-emerald-600 dark:text-emerald-400" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-semibold text-sm text-foreground">Vídeo enviado!</p>
                <p class="text-xs text-muted-foreground truncate">{{ fileName() }}</p>
              </div>
            </div>
          }

          @case ('error') {
            <div class="flex items-center gap-4 p-5 pointer-events-none">
              <div class="rounded-xl bg-destructive/10 p-3 shrink-0">
                <z-icon zType="circle-alert" zSize="default" class="text-destructive" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-semibold text-sm text-foreground">Falha no upload</p>
                <p class="text-xs text-muted-foreground truncate">{{ fileName() }}</p>
              </div>
            </div>
          }
        }
      </div>

      <!-- Validation error -->
      @if (validationError()) {
        <z-alert
          zType="destructive"
          zTitle="Arquivo inválido"
          [zDescription]="validationError()!"
        />
      }

      <!-- Resolution selector -->
      @if (showResolutionSelector()) {
        <div class="space-y-2">
          <label class="text-sm font-medium">Resoluções para processamento</label>
          <z-select
            [zValue]="uploadResolutions()"
            [zMultiple]="true"
            zPlaceholder="Selecione resoluções"
            (zSelectionChange)="onResolutionsChange($event)"
          >
            <z-select-item zValue="1080p">1080p — Full HD</z-select-item>
            <z-select-item zValue="720p">720p — HD</z-select-item>
            <z-select-item zValue="480p">480p — SD</z-select-item>
            <z-select-item zValue="360p">360p — Baixa</z-select-item>
          </z-select>
        </div>
      }

      <!-- Progress detail -->
      @if (state() === 'uploading') {
        <div class="rounded-xl border bg-card p-4 space-y-3 text-sm">
          @for (stage of uploadStages; track stage.key) {
            <div class="flex items-center gap-3">
              <div class="shrink-0 w-5 h-5 flex items-center justify-center">
                @if (currentStage() === stage.key) {
                  <z-icon zType="loader-circle" zSize="sm" class="animate-spin text-primary" />
                } @else if (isStageComplete(stage.key)) {
                  <z-icon zType="circle-check" zSize="sm" class="text-emerald-500" />
                } @else {
                  <z-icon zType="circle-small" zSize="sm" class="text-muted-foreground/40" />
                }
              </div>
              <span [class]="stageTextClass(stage.key)">{{ stage.label }}</span>
              @if (stage.key === 'chunks' && currentStage() === 'chunks') {
                <span class="ml-auto text-xs text-muted-foreground font-mono">
                  {{ currentChunk() }}/{{ totalChunks() }}
                </span>
              }
            </div>
          }
        </div>
      }

      <!-- Upload error detail -->
      @if (state() === 'error') {
        <z-alert
          zType="destructive"
          zTitle="Erro ao enviar vídeo"
          [zDescription]="errorMessage()"
        />
      }

      <!-- Success detail -->
      @if (state() === 'success') {
        <div class="rounded-xl border bg-card p-4 space-y-2 text-sm">
          <p class="font-medium text-foreground">Detalhes do vídeo</p>
          <div class="space-y-1 text-muted-foreground">
            <p><span class="font-medium text-foreground">ID:</span> {{ uploadedVideoId() }}</p>
            <p><span class="font-medium text-foreground">Status:</span> {{ uploadedVideoStatus() }}</p>
            <p class="text-xs pt-1">O vídeo está sendo processado nas resoluções selecionadas.</p>
          </div>
        </div>
      }

      <!-- Actions -->
      <div class="flex items-center gap-3">
        @if (state() !== 'success') {
          <z-button
            [zDisabled]="!canUpload()"
            [zLoading]="state() === 'uploading'"
            zType="default"
            class="flex-1"
            (click)="onUpload()"
          >
            @if (state() !== 'uploading') {
              <z-icon zType="cloud-upload" />
            }
            {{ state() === 'uploading' ? 'Enviando...' : 'Enviar vídeo' }}
          </z-button>

          @if (state() === 'error') {
            <z-button zType="outline" (click)="onUpload()">
              Tentar novamente
            </z-button>
          }
        } @else {
          <z-button zType="outline" class="w-full" (click)="reset()">
            <z-icon zType="plus" />
            Enviar outro vídeo
          </z-button>
        }
      </div>

    </div>
  `,
})
export class UploadComponent {
  protected readonly state = signal<UploadState>('idle');
  protected readonly uploadProgress = signal(0);
  protected readonly currentChunk = signal(0);
  protected readonly totalChunks = signal(0);
  protected readonly uploadStatus = signal('');
  protected readonly errorMessage = signal('');
  protected readonly validationError = signal<string | null>(null);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly uploadedVideoId = signal<string | null>(null);
  protected readonly uploadedVideoStatus = signal<string | null>(null);
  protected readonly uploadResolutions = signal<Resolution[]>(['1080p', '720p', '480p']);
  protected readonly currentStage = signal<'init' | 'chunks' | 'complete'>('init');

  protected readonly uploadStages = [
    { key: 'init' as const, label: 'Iniciando sessão de upload' },
    { key: 'chunks' as const, label: 'Enviando chunks' },
    { key: 'complete' as const, label: 'Finalizando upload' },
  ];

  protected readonly canUpload = computed(
    () =>
      (this.state() === 'file-selected' || this.state() === 'error') &&
      !!this.selectedFile() &&
      !this.validationError(),
  );

  protected readonly showResolutionSelector = computed(
    () => this.state() === 'file-selected' || this.state() === 'error',
  );

  protected readonly fileName = computed(() => this.selectedFile()?.name ?? '');
  protected readonly fileSize = computed(() =>
    this.formatBytes(this.selectedFile()?.size ?? 0),
  );

  private readonly uploadService = inject(UploadService);
  private readonly authStore = inject(AuthStore);

  dropZoneClasses(isDragging: boolean): string {
    const base = 'drop-zone';
    if (this.state() === 'uploading') {
      return `${base} border-primary/30 bg-primary/5 cursor-default`;
    }
    if (this.state() === 'success') {
      return `${base} border-emerald-500/30 bg-emerald-500/5 cursor-default`;
    }
    if (this.state() === 'error') {
      return `${base} border-destructive/30 bg-destructive/5`;
    }
    if (isDragging) {
      return `${base} border-primary bg-primary/5`;
    }
    if (this.state() === 'file-selected') {
      return `${base} border-primary/40 bg-card`;
    }
    return `${base} border-border hover:border-primary/50 hover:bg-muted/50`;
  }

  isStageComplete(key: 'init' | 'chunks' | 'complete'): boolean {
    const order = { init: 0, chunks: 1, complete: 2 };
    return order[this.currentStage()] > order[key];
  }

  stageTextClass(key: 'init' | 'chunks' | 'complete'): string {
    if (this.currentStage() === key) return 'text-foreground font-medium';
    if (this.isStageComplete(key)) return 'text-muted-foreground';
    return 'text-muted-foreground/50';
  }

  onFilesDropped(files: File[]): void {
    if (this.state() === 'uploading') return;
    const [file] = files;
    if (file) this.applyFile(file);
  }

  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const [file] = input?.files ?? [];
    if (file) this.applyFile(file);
    if (input) input.value = '';
  }

  private applyFile(file: File): void {
    this.validationError.set(null);

    if (!this.isVideoFile(file)) {
      this.validationError.set(
        `Tipo de arquivo não suportado: ${file.type || 'desconhecido'}. Use MP4, MKV, MOV, AVI ou WebM.`,
      );
      return;
    }

    if (file.size > MAX_FILE_SIZE_BYTES) {
      this.validationError.set(
        `Arquivo muito grande (${this.formatBytes(file.size)}). Limite máximo: 10 GB.`,
      );
      return;
    }

    this.selectedFile.set(file);
    this.uploadProgress.set(0);
    this.errorMessage.set('');
    this.state.set('file-selected');
  }

  clearFile(event: MouseEvent): void {
    event.stopPropagation();
    this.reset();
  }

  async onUpload(): Promise<void> {
    const file = this.selectedFile();
    if (!file || !this.canUpload()) return;

    if (!this.authStore.creator()?.id) {
      this.errorMessage.set('Usuário não autenticado. Faça login para enviar vídeos.');
      this.state.set('error');
      return;
    }

    this.state.set('uploading');
    this.uploadProgress.set(0);
    this.currentChunk.set(0);
    this.errorMessage.set('');
    this.currentStage.set('init');
    this.uploadStatus.set('Iniciando sessão...');

    try {
      const uploadData = await lastValueFrom(
        this.uploadService.initiateChunkedUpload({
          fileName: file.name,
          fileSize: file.size,
          mimeType: file.type,
          resolutions: this.uploadResolutions(),
        }),
      );

      this.uploadedVideoId.set(uploadData.videoId);
      this.totalChunks.set(uploadData.totalChunks);
      this.currentStage.set('chunks');

      await lastValueFrom(
        this.uploadService
          .uploadChunks({
            file,
            uploadId: uploadData.uploadId,
            totalChunks: uploadData.totalChunks,
            chunks: uploadData.chunks,
          })
          .pipe(
            tap(({ chunkNumber, totalChunks, progress }) => {
              this.currentChunk.set(chunkNumber);
              this.uploadProgress.set(progress);
              this.uploadStatus.set(`Chunk ${chunkNumber} de ${totalChunks}`);
            }),
          ),
      );

      this.currentStage.set('complete');
      this.uploadStatus.set('Finalizando...');
      this.uploadProgress.set(99);

      const completed = await lastValueFrom(
        this.uploadService.completeChunkedUpload(uploadData.uploadId),
      );

      this.uploadedVideoId.set(completed.videoId);
      this.uploadedVideoStatus.set(completed.status);
      this.uploadProgress.set(100);
      this.state.set('success');
    } catch (error) {
      this.errorMessage.set(
        error instanceof Error ? error.message : 'Erro inesperado. Tente novamente.',
      );
      this.state.set('error');
    }
  }

  onResolutionsChange(value: string | string[]): void {
    if (!Array.isArray(value) || value.length === 0) return;
    const resolutions = value.filter(this.isResolution) as Resolution[];
    if (resolutions.length > 0) this.uploadResolutions.set(resolutions);
  }

  reset(): void {
    this.state.set('idle');
    this.selectedFile.set(null);
    this.uploadProgress.set(0);
    this.currentChunk.set(0);
    this.totalChunks.set(0);
    this.uploadStatus.set('');
    this.errorMessage.set('');
    this.validationError.set(null);
    this.uploadedVideoId.set(null);
    this.uploadedVideoStatus.set(null);
    this.currentStage.set('init');
  }

  private isVideoFile(file: File): boolean {
    if (file.type && VIDEO_MIME_TYPES.has(file.type)) return true;
    const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
    return ['mp4', 'mkv', 'mov', 'avi', 'webm', 'ogg', 'flv', 'wmv'].includes(ext);
  }

  private isResolution(value: string): value is Resolution {
    return value === '360p' || value === '480p' || value === '720p' || value === '1080p';
  }

  private formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const unit = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const index = Math.floor(Math.log(bytes) / Math.log(unit));
    const value = bytes / unit ** index;
    return `${value.toFixed(value < 10 && index > 0 ? 2 : 0)} ${sizes[index]}`;
  }
}
