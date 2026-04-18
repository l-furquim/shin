import { AuthStore } from '@/core/stores/auth.store';
import { httpResource } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { SearchVideosResponse } from '../videos/video.types';
import { VideoService } from '../videos/video.service';
import { Creator } from '../creator/creator.types';
import { SidebarComponent } from '@/shared/components/sidebar/sidebar.component';
import { VideoCardSkeletonComponent } from '@/shared/components/video/video-card-skeleton.component';
import { StudioVideoComponent } from './studio-video.component';

@Component({
  selector: 'app-studio',
  template: `
    <div class="min-h-screen md:flex">
      <app-sidebar></app-sidebar>
      <main class="w-full px-4 py-10 md:px-8">
        <div class="mx-auto flex w-full max-w-7xl flex-col gap-8 lg:gap-10">
          <section class="space-y-3">
            <h1 class="text-3xl font-semibold tracking-tight md:text-4xl">Studio</h1>
          </section>

          <section class="w-full flex flex-col gap-5 items-center">
            @if (videos.isLoading()) {
              @for (item of [].constructor(20); track $index) {
                <video-card-skeleton></video-card-skeleton>
              }
            }
            @if (videos.error()) {
              <p class="text-destructive col-span-2">Erro ao carregar vídeos.</p>
            }
            @if (videos.hasValue()) {
              @for (video of videos.value().items; track video.id) {
                <studio-video [video]="video"></studio-video>
              } @empty {
                <p class="text-muted-foreground col-span-2">Nenhum vídeo encontrado.</p>
              }
            }
          </section>
        </div>
      </main>
    </div>
  `,
  imports: [SidebarComponent, VideoCardSkeletonComponent, StudioVideoComponent],
})
export class Studio {
  readonly authStore = inject(AuthStore);
  private readonly videosService = inject(VideoService);
  private readonly creator = signal<Creator | null>(this.authStore.creator());

  protected readonly cursor = signal<string | undefined>(undefined);

  protected readonly videos = httpResource<SearchVideosResponse>(() =>
    this.videosService.searchVideos({
      channelId: this.creator()?.id,
      fields: 'contentDetails,statistics,thumbnails, channel',
      limit: 20,
      cursor: this.cursor(),
    }),
  );

  protected onNext(token: string): void {
    this.cursor.set(token);
  }

  protected onPrev(token: string): void {
    this.cursor.set(token);
  }
}
