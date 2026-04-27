import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { SidebarComponent } from '@/shared/components/sidebar/sidebar.component';
import { UploadComponent } from '@/shared/components/upload/upload.component';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardInputDirective } from '@/shared/components/input';
import { ZardSelectComponent, ZardSelectItemComponent } from '@/shared/components/select';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardIconComponent } from '@/shared/components/icon';
import { TagInputComponent } from '@/shared/components/tag-input';
import { CategoryComboboxComponent } from '@/shared/components/category-combobox';
import { VideoService } from '@/features/videos/video.service';
import type { VideoVisibility } from '@/features/videos/video.types';
import { Router } from '@angular/router';

@Component({
  selector: 'app-upload-page',
  imports: [
    SidebarComponent,
    UploadComponent,
    ReactiveFormsModule,
    ZardButtonComponent,
    ZardInputDirective,
    ZardSelectComponent,
    ZardSelectItemComponent,
    ZardAlertComponent,
    ZardIconComponent,
    TagInputComponent,
    CategoryComboboxComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .layout-wrapper {
      transition: max-width 0.3s ease;
    }
  `,
  template: `
    <div class="min-h-screen md:flex">
      <app-sidebar></app-sidebar>
      <main class="w-full px-4 py-10 md:px-8">
        <div class="layout-wrapper mx-auto w-full" [class]="hasStartedUpload() ? 'max-w-5xl' : 'max-w-2xl'">
          <section class="mb-8 space-y-1">
            <h1 class="text-3xl font-semibold tracking-tight md:text-4xl">Upload de vídeo</h1>
            <p class="text-muted-foreground text-sm">
              Envie seu vídeo e preencha os detalhes enquanto ele é processado.
            </p>
          </section>

          <div
            [class]="
              hasStartedUpload()
                ? 'grid grid-cols-1 items-start gap-8 lg:grid-cols-[1fr_360px]'
                : 'flex flex-col gap-6'
            "
          >
            @if (hasStartedUpload()) {
              <div class="space-y-5 rounded-2xl border bg-card p-6">
                <div class="space-y-0.5">
                  <p class="font-semibold">Detalhes do vídeo</p>
                  <p class="text-muted-foreground text-xs">
                    Preencha enquanto o upload acontece em segundo plano.
                  </p>
                </div>

                <form [formGroup]="form" class="space-y-5">
                  <div class="space-y-1.5">
                    <label for="title" class="text-sm font-medium">Título</label>
                    <input
                      id="title"
                      z-input
                      type="text"
                      formControlName="title"
                      placeholder="Nome do seu vídeo"
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
                    <p class="text-muted-foreground text-xs">
                      Digite e pressione Enter para criar uma nova tag.
                    </p>
                  </div>

                  @if (saveError()) {
                    <z-alert zType="destructive" [zDescription]="saveError()" />
                  }

                  <z-button
                    [zDisabled]="form.invalid || isSaving() || !activeVideoId() || !readyToPublish()"
                    [zLoading]="isSaving()"
                    class="w-full"
                    (click)="onSaveDetails()"
                  >
                    @if (!isSaving()) {
                      <z-icon zType="cloud-upload" zSize="sm" />
                    }
                    {{ isSaving() ? 'Publicando...' : 'Publicar vídeo' }}
                  </z-button>

                  @if (!activeVideoId()) {
                    <p class="text-muted-foreground text-center text-xs">
                      Disponível após iniciar o upload.
                    </p>
                  }
                </form>
              </div>
            }

            <div [class]="hasStartedUpload() ? 'lg:sticky lg:top-6' : ''">
              <upload-area
                (fileSelected)="onFileSelected()"
                (uploadStarted)="onUploadStarted()"
                (videoIdReady)="onVideoIdReady($event)"
                (videoReady)="onReadyToPublish($event)"
              ></upload-area>
            </div>
          </div>
        </div>
      </main>
    </div>
  `,
})
export class UploadPageComponent {
  private readonly router = inject(Router);
  private readonly videoService = inject(VideoService);
  private readonly fb = inject(FormBuilder);

  protected readonly readyToPublish = signal(false);
  protected readonly hasFile = signal(false);
  protected readonly hasStartedUpload = signal(false);
  protected readonly activeVideoId = signal<string | null>(null);
  protected readonly isSaving = signal(false);
  protected readonly saveError = signal('');

  protected readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    visibility: ['PRIVATE' as VideoVisibility],
    categoryId: [null as number | null],
    tags: [[] as string[]],
  });

  onReadyToPublish(ready: boolean) {
    this.readyToPublish.set(ready);
  }

  onFileSelected(): void {
    this.hasFile.set(true);
    this.readyToPublish.set(false);
    this.activeVideoId.set(null);
    this.saveError.set('');
  }

  onUploadStarted(): void {
    this.hasStartedUpload.set(true);
  }

  onVideoIdReady(id: string): void {
    this.activeVideoId.set(id);
    this.readyToPublish.set(false);
  }

  onVisibilityChange(value: string | string[]): void {
    const v = Array.isArray(value) ? value[0] : value;
    if (v === 'PUBLIC' || v === 'PRIVATE') {
      this.form.controls.visibility.setValue(v);
    }
  }

  async onSaveDetails(): Promise<void> {
    if (this.form.invalid || this.isSaving() || !this.activeVideoId() || !this.readyToPublish()) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.saveError.set('');

    const { title, description, visibility, categoryId, tags } = this.form.controls;

    try {
      await firstValueFrom(
        this.videoService.patchVideo(this.activeVideoId()!, {
          title: title.value,
          description: description.value || undefined,
          categoryId: categoryId.value ?? undefined,
          tagsToAdd: tags.value.map((name) => ({ name })),
        }),
      );

      if (visibility.value === 'PUBLIC') {
        await firstValueFrom(this.videoService.publish(this.activeVideoId()!));
      }

      await this.router.navigate(['/videos', this.activeVideoId(), 'manage']);
    } catch {
      this.saveError.set('Não foi possível publicar o vídeo. Tente novamente.');
      this.isSaving.set(false);
    }
  }
}
