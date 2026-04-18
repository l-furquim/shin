import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { catchError, of } from 'rxjs';
import { SidebarComponent } from '@/shared/components/sidebar/sidebar.component';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardInputDirective } from '@/shared/components/input';
import { ZardSelectComponent, ZardSelectItemComponent } from '@/shared/components/select';
import { TagInputComponent } from '@/shared/components/tag-input';
import { CategoryComboboxComponent } from '@/shared/components/category-combobox';
import { VideoService } from '@/features/videos/video.service';
import type { VideoItem, VideoVisibility } from '@/features/videos/video.types';

type Tab = 'overview' | 'details' | 'media';

@Component({
  selector: 'app-video-manage',
  imports: [
    SidebarComponent,
    ReactiveFormsModule,
    DatePipe,
    DecimalPipe,
    ZardButtonComponent,
    ZardBadgeComponent,
    ZardIconComponent,
    ZardSkeletonComponent,
    ZardAlertComponent,
    ZardInputDirective,
    ZardSelectComponent,
    ZardSelectItemComponent,
    TagInputComponent,
    CategoryComboboxComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen md:flex">
      <app-sidebar />
      <main class="w-full px-4 py-10 md:px-8">
        <div class="mx-auto max-w-4xl space-y-8">
          <button
            type="button"
            class="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
            (click)="router.navigate(['/dashboard'])"
          >
            <z-icon zType="chevron-left" zSize="sm" />
            Meus vídeos
          </button>

          @if (loading()) {
            <div class="space-y-4">
              <z-skeleton class="h-8 w-64 rounded-md" />
              <z-skeleton class="h-4 w-40 rounded-md" />
              <z-skeleton class="h-10 w-full rounded-md" />
              <z-skeleton class="h-48 w-full rounded-md" />
            </div>
          } @else if (loadError()) {
            <z-alert zType="destructive" zTitle="Erro" [zDescription]="loadError()" />
          } @else {
            <div class="flex flex-col gap-1 sm:flex-row sm:items-start sm:justify-between">
              <div class="space-y-0.5">
                <h1 class="text-2xl font-semibold tracking-tight">{{ video()?.title }}</h1>
                <p class="text-sm text-muted-foreground">
                  @if (video()?.publishedAt) {
                    Publicado em {{ video()?.publishedAt | date: 'dd/MM/yyyy' }}
                  } @else {
                    Criado em {{ video()?.createdAt | date: 'dd/MM/yyyy' }}
                  }
                </p>
              </div>
              <z-badge [zType]="statusBadgeType()">{{ statusLabel() }}</z-badge>
            </div>

            <div class="border-b -mb-4">
              <nav class="flex gap-6">
                @for (tab of tabs; track tab.id) {
                  <button
                    type="button"
                    class="pb-3 text-sm font-medium border-b-2 transition-colors hover:text-foreground"
                    [class]="
                      activeTab() === tab.id
                        ? 'border-foreground text-foreground'
                        : 'border-transparent text-muted-foreground'
                    "
                    (click)="activeTab.set(tab.id)"
                  >
                    {{ tab.label }}
                  </button>
                }
              </nav>
            </div>

            @if (activeTab() === 'overview') {
              <div class="space-y-6 pt-4">
                <div class="grid grid-cols-3 gap-4">
                  <div class="rounded-xl border p-5 space-y-1">
                    <p class="text-xs text-muted-foreground">Visualizações</p>
                    <p class="text-2xl font-semibold">
                      {{ video()?.statistics?.viewCount ?? 0 | number }}
                    </p>
                  </div>
                  <div class="rounded-xl border p-5 space-y-1">
                    <p class="text-xs text-muted-foreground">Curtidas</p>
                    <p class="text-2xl font-semibold">
                      {{ video()?.statistics?.likeCount ?? 0 | number }}
                    </p>
                  </div>
                  <div class="rounded-xl border p-5 space-y-1">
                    <p class="text-xs text-muted-foreground">Comentários</p>
                    <p class="text-2xl font-semibold">
                      {{ video()?.statistics?.commentCount ?? 0 | number }}
                    </p>
                  </div>
                </div>

                <div class="rounded-xl border p-5 space-y-3">
                  <div class="flex items-center justify-between">
                    <p class="text-sm font-medium">Status de processamento</p>
                    <z-badge [zType]="statusBadgeType()">{{ statusLabel() }}</z-badge>
                  </div>
                  @if (video()?.processingDetails?.processingProgress != null) {
                    <div class="space-y-1.5">
                      <div class="flex justify-between text-xs text-muted-foreground">
                        <span>Progresso</span>
                        <span>{{ video()?.processingDetails?.processingProgress ?? 0 }}%</span>
                      </div>
                      <div class="h-1.5 w-full rounded-full bg-stone-100">
                        <div
                          class="h-1.5 rounded-full bg-foreground transition-all duration-500"
                          [style.width.%]="video()?.processingDetails?.processingProgress ?? 0"
                        ></div>
                      </div>
                    </div>
                  }
                  @if (video()?.processingDetails?.processingFailureReason) {
                    <p class="text-xs text-destructive">
                      {{ video()?.processingDetails?.processingFailureReason }}
                    </p>
                  }
                </div>

                <div class="rounded-xl border p-5 space-y-3">
                  <p class="text-sm font-medium">Link do vídeo</p>
                  <div class="flex items-center gap-2">
                    <div
                      class="flex-1 rounded-lg border bg-stone-50 px-3 py-2 text-sm text-muted-foreground font-mono truncate"
                    >
                      {{ videoLink() }}
                    </div>
                    <z-button zType="outline" (click)="copyLink()">
                      @if (linkCopied()) {
                        <z-icon zType="check" zSize="sm" />
                        Copiado
                      } @else {
                        <z-icon zType="copy" zSize="sm" />
                        Copiar
                      }
                    </z-button>
                  </div>
                </div>
              </div>
            }

            @if (activeTab() === 'details') {
              <div class="space-y-6 pt-4">
                <form [formGroup]="form" class="rounded-xl border p-6 space-y-5">
                  <div>
                    <p class="font-medium">Metadados</p>
                    <p class="text-xs text-muted-foreground mt-0.5">
                      Edite as informações do vídeo. As alterações serão aplicadas imediatamente.
                    </p>
                  </div>

                  <div class="space-y-1.5">
                    <label for="title" class="text-sm font-medium">Título</label>
                    <input
                      id="title"
                      z-input
                      type="text"
                      formControlName="title"
                      placeholder="Nome do vídeo"
                      class="w-full"
                    />
                    @if (form.controls.title.invalid && form.controls.title.touched) {
                      <p class="text-destructive text-xs">
                        Título é obrigatório (máx. 200 caracteres).
                      </p>
                    }
                  </div>

                  <div class="space-y-1.5">
                    <label for="description" class="text-sm font-medium">
                      Descrição
                      <span class="text-muted-foreground ml-1 font-normal">(opcional)</span>
                    </label>
                    <textarea
                      id="description"
                      z-input
                      formControlName="description"
                      placeholder="Descreva seu vídeo..."
                      rows="5"
                      class="w-full resize-none"
                    ></textarea>
                  </div>

                  <div class="space-y-1.5">
                    <label class="text-sm font-medium">Visibilidade</label>
                    <z-select
                      [zValue]="form.controls.visibility.value"
                      (zSelectionChange)="onVisibilityChange($event)"
                      zPlaceholder="Selecione a visibilidade"
                    >
                      <z-select-item zValue="PRIVATE">
                        <span class="flex items-center gap-2">
                          <z-icon zType="shield" zSize="sm" />
                          Privado
                        </span>
                      </z-select-item>
                      <z-select-item zValue="NOT_LISTED">
                        <span class="flex items-center gap-2">
                          <z-icon zType="share" zSize="sm" />
                          Não listado
                        </span>
                      </z-select-item>
                      <z-select-item zValue="PUBLIC">
                        <span class="flex items-center gap-2">
                          <z-icon zType="eye" zSize="sm" />
                          Público
                        </span>
                      </z-select-item>
                    </z-select>
                  </div>

                  <div class="space-y-1.5">
                    <label class="text-sm font-medium">
                      Categoria
                      <span class="text-muted-foreground ml-1 font-normal">(opcional)</span>
                    </label>
                    <z-category-combobox formControlName="categoryId" />
                  </div>

                  <div class="space-y-1.5">
                    <label class="text-sm font-medium">
                      Tags
                      <span class="text-muted-foreground ml-1 font-normal">(opcional)</span>
                    </label>
                    <z-tag-input formControlName="tags" />
                  </div>

                  @if (saveError()) {
                    <z-alert zType="destructive" [zDescription]="saveError()" />
                  }
                  @if (saveSuccess()) {
                    <z-alert
                      zType="default"
                      zTitle="Salvo!"
                      zDescription="Metadados atualizados com sucesso."
                    />
                  }

                  <z-button
                    [zDisabled]="form.invalid || isSaving()"
                    [zLoading]="isSaving()"
                    (click)="onSave()"
                  >
                    @if (!isSaving()) {
                      <z-icon zType="save" zSize="sm" />
                    }
                    {{ isSaving() ? 'Salvando...' : 'Salvar alterações' }}
                  </z-button>
                </form>
              </div>
            }

            @if (activeTab() === 'media') {
              <div class="space-y-6 pt-4">
                <div class="rounded-xl border p-6 space-y-4">
                  <div>
                    <p class="font-medium">Thumbnail</p>
                    <p class="text-xs text-muted-foreground mt-0.5">
                      Imagem de capa exibida no feed e na página do vídeo.
                    </p>
                  </div>

                  <div class="flex flex-col sm:flex-row items-start gap-4">
                    @if (thumbnailUrl()) {
                      <img
                        [src]="thumbnailUrl()"
                        alt="Thumbnail do vídeo"
                        class="w-full sm:w-48 rounded-lg border object-cover aspect-video"
                      />
                    } @else {
                      <div
                        class="w-full sm:w-48 aspect-video rounded-lg border bg-stone-100 flex items-center justify-center"
                      >
                        <z-icon zType="monitor" class="text-muted-foreground" />
                      </div>
                    }

                    <div class="space-y-2">
                      <p class="text-sm text-muted-foreground">
                        Formatos aceitos: JPG, PNG, WebP. Tamanho máximo: 2 MB.
                      </p>
                      <z-button zType="outline" zDisabled="true">
                        <z-icon zType="cloud-upload" zSize="sm" />
                        Alterar thumbnail
                      </z-button>
                      <p class="text-xs text-muted-foreground">Em desenvolvimento.</p>
                    </div>
                  </div>
                </div>

                <div class="rounded-xl border p-6 space-y-4">
                  <div>
                    <p class="font-medium">Resoluções disponíveis</p>
                    <p class="text-xs text-muted-foreground mt-0.5">
                      Versões do vídeo geradas após o processamento.
                    </p>
                  </div>

                  @if (parsedResolutions().length) {
                    <div class="divide-y rounded-lg border">
                      <div
                        class="grid grid-cols-2 px-4 py-2.5 text-xs font-medium text-muted-foreground"
                      >
                        <span>Resolução</span>
                        <span>Status</span>
                      </div>
                      @for (res of parsedResolutions(); track res) {
                        <div class="grid grid-cols-2 px-4 py-3 text-sm items-center">
                          <span class="font-medium">{{ res }}</span>
                          <span class="flex items-center gap-1.5 text-xs text-green-600">
                            <span class="h-1.5 w-1.5 rounded-full bg-green-500"></span>
                            Disponível
                          </span>
                        </div>
                      }
                    </div>
                  } @else {
                    <p class="text-sm text-muted-foreground py-2">
                      Nenhuma resolução disponível. O processamento pode ainda estar em andamento.
                    </p>
                  }
                </div>

                @if (video()?.fileDetails) {
                  <div class="rounded-xl border p-6 space-y-4">
                    <p class="font-medium">Detalhes do arquivo</p>
                    <dl class="grid grid-cols-1 gap-3 sm:grid-cols-3">
                      <div class="space-y-0.5">
                        <dt class="text-xs text-muted-foreground">Nome do arquivo</dt>
                        <dd class="text-sm font-medium truncate">
                          {{ video()?.fileDetails?.fileName }}
                        </dd>
                      </div>
                      <div class="space-y-0.5">
                        <dt class="text-xs text-muted-foreground">Tamanho</dt>
                        <dd class="text-sm font-medium">
                          {{ formatFileSize(video()?.fileDetails?.fileSize ?? 0) }}
                        </dd>
                      </div>
                      <div class="space-y-0.5">
                        <dt class="text-xs text-muted-foreground">Tipo</dt>
                        <dd class="text-sm font-medium">{{ video()?.fileDetails?.fileType }}</dd>
                      </div>
                    </dl>
                  </div>
                }

                @if (video()?.contentDetails?.duration) {
                  <div class="rounded-xl border p-6 space-y-3">
                    <p class="font-medium">Informações técnicas</p>
                    <dl class="grid grid-cols-2 gap-3 sm:grid-cols-3">
                      <div class="space-y-0.5">
                        <dt class="text-xs text-muted-foreground">Duração</dt>
                        <dd class="text-sm font-medium">
                          {{ formatDuration(video()?.contentDetails?.duration ?? 0) }}
                        </dd>
                      </div>
                    </dl>
                  </div>
                }
              </div>
            }
          }
        </div>
      </main>
    </div>
  `,
})
export class VideoManageComponent implements OnInit {
  protected readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly videoService = inject(VideoService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);

  protected readonly video = signal<VideoItem | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal('');
  protected readonly activeTab = signal<Tab>('overview');
  protected readonly linkCopied = signal(false);

  protected readonly isSaving = signal(false);
  protected readonly saveSuccess = signal(false);
  protected readonly saveError = signal('');

  private readonly originalTags = signal<string[]>([]);

  protected readonly tabs: { id: Tab; label: string }[] = [
    { id: 'overview', label: 'Visão geral' },
    { id: 'details', label: 'Detalhes' },
    { id: 'media', label: 'Mídia' },
  ];

  protected readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    visibility: ['PRIVATE' as VideoVisibility],
    categoryId: [null as number | null],
    tags: [[] as string[]],
  });

  private videoId = '';

  ngOnInit(): void {
    this.videoId = this.route.snapshot.params['id'];
    this.loadVideo();
  }

  private loadVideo(): void {
    this.videoService
      .getVideo(this.videoId, 'contentDetails,statistics,fileDetails,processingDetails,thumbnails')
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError((err) => {
          this.loadError.set(err?.message ?? 'Não foi possível carregar o vídeo.');
          this.loading.set(false);
          return of(null);
        }),
      )
      .subscribe((video) => {
        if (!video) return;
        this.video.set(video);
        this.loading.set(false);
        this.populateForm(video);
      });
  }

  private populateForm(video: VideoItem): void {
    const tags = Array.from(video.tags ?? []);
    this.originalTags.set(tags);
    this.form.patchValue({
      title: video.title,
      description: video.description ?? '',
      visibility: video.visibility,
      categoryId: video.categoryId ? Number(video.categoryId) : null,
      tags,
    });
  }

  protected onVisibilityChange(value: string | string[]): void {
    const v = Array.isArray(value) ? value[0] : value;
    if (v === 'PUBLIC' || v === 'PRIVATE' || v === 'NOT_LISTED') {
      this.form.controls.visibility.setValue(v);
    }
  }

  protected async onSave(): Promise<void> {
    if (this.form.invalid || this.isSaving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.saveSuccess.set(false);
    this.saveError.set('');

    const { title, description, visibility, categoryId, tags } = this.form.controls;
    const newTags = tags.value;
    const original = this.originalTags();
    const tagsToAdd = newTags.filter((t) => !original.includes(t)).map((name) => ({ name }));
    const tagsToRemove = original.filter((t) => !newTags.includes(t)).map((name) => ({ name }));

    try {
      const updated = await firstValueFrom(
        this.videoService.patchVideo(this.videoId, {
          title: title.value,
          description: description.value || undefined,
          visibility: visibility.value,
          categoryId: categoryId.value ?? undefined,
          tagsToAdd: tagsToAdd.length ? tagsToAdd : undefined,
          tagsToRemove: tagsToRemove.length ? tagsToRemove : undefined,
        }),
      );
      this.video.set(updated);
      this.originalTags.set(newTags);
      this.saveSuccess.set(true);
      setTimeout(() => this.saveSuccess.set(false), 4000);
    } catch {
      this.saveError.set('Não foi possível salvar as alterações. Tente novamente.');
    } finally {
      this.isSaving.set(false);
    }
  }

  protected thumbnailUrl(): string | null {
    const t = this.video()?.thumbnails;
    if (!t) return null;
    return t['maxres']?.url ?? t['high']?.url ?? t['medium']?.url ?? t['default']?.url ?? null;
  }

  protected parsedResolutions(): string[] {
    const res = this.video()?.contentDetails?.resolutions;
    if (!res) return [];
    return res
      .split(',')
      .map((r) => r.trim())
      .filter(Boolean);
  }

  protected videoLink(): string {
    return `${window.location.origin}/videos/${this.videoId}`;
  }

  protected copyLink(): void {
    navigator.clipboard.writeText(this.videoLink()).then(() => {
      this.linkCopied.set(true);
      setTimeout(() => this.linkCopied.set(false), 2000);
    });
  }

  protected statusLabel(): string {
    const s = this.video()?.processingDetails?.processingStatus;
    const map: Record<string, string> = {
      UPLOADING: 'Enviando',
      UPLOADED: 'Enviado',
      PROCESSING: 'Processando',
      PROCESSED: 'Pronto',
      DRAFT: 'Rascunho',
      FAILED: 'Falhou',
      EXPIRED: 'Expirado',
    };
    return map[s ?? ''] ?? s ?? 'Desconhecido';
  }

  protected statusBadgeType(): 'default' | 'secondary' | 'destructive' | 'outline' {
    const s = this.video()?.processingDetails?.processingStatus;
    if (s === 'PROCESSED') return 'default';
    if (s === 'FAILED' || s === 'EXPIRED') return 'destructive';
    if (s === 'PROCESSING' || s === 'UPLOADING' || s === 'UPLOADED') return 'secondary';
    return 'outline';
  }

  protected formatFileSize(bytes: number): string {
    if (bytes === 0) return '—';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`;
    if (bytes < 1024 ** 3) return `${(bytes / 1024 ** 2).toFixed(1)} MB`;
    return `${(bytes / 1024 ** 3).toFixed(2)} GB`;
  }

  protected formatDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    return `${m}:${String(s).padStart(2, '0')}`;
  }
}
