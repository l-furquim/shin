import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SidebarComponent } from '@/shared/components/sidebar/sidebar.component';
import { UploadComponent } from '@/shared/components/upload/upload.component';

@Component({
  selector: 'app-upload-page',
  imports: [SidebarComponent, UploadComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen md:flex">
      <app-sidebar></app-sidebar>
      <main class="w-full px-4 py-10 md:px-8">
        <div class="mx-auto flex w-full max-w-2xl flex-col gap-8">
          <section class="space-y-2">
            <h1 class="text-3xl font-semibold tracking-tight md:text-4xl">Upload de vídeo</h1>
            <p class="text-muted-foreground">
              Envie seu vídeo e ele será processado nas resoluções selecionadas.
            </p>
          </section>

          <upload-area></upload-area>
        </div>
      </main>
    </div>
  `,
})
export class UploadPageComponent {}
