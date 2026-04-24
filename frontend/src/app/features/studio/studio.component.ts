import { AuthStore } from '@/core/stores/auth.store';
import { httpResource } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { SearchVideosResponse } from '../videos/video.types';
import { VideoService } from '../videos/video.service';
import { Creator } from '../creator/creator.types';
import { SidebarComponent } from '@/shared/components/sidebar/sidebar.component';
import { StudioVideoSkeletonComponent } from './studio-video-skeleton.component';
import { StudioVideoComponent } from './studio-video.component';
import { PaginationComponent } from '@/shared/components/pagination/pagination.component';

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

          <section class="w-full flex flex-col gap-3">
            @if (videos.isLoading()) {
              @for (item of [].constructor(10); track $index) {
                <studio-video-skeleton></studio-video-skeleton>
              }
            }
            @if (videos.error()) {
              <p class="text-destructive">Erro ao carregar vídeos.</p>
            }
            @if (videos.hasValue()) {
              @for (video of videos.value().items; track video.id) {
                <studio-video [video]="video"></studio-video>
              } @empty {
                <p class="text-muted-foreground">Nenhum vídeo encontrado.</p>
              }
              <app-pagination
                [nextPageToken]="videos.value().nextPageToken || null"
                [prevPageToken]="videos.value().prevPageToken || null"
                [loading]="videos.isLoading()"
                (next)="onNext($event)"
                (prev)="onPrev($event)"
              />
            }
          </section>
        </div>
      </main>
    </div>
  `,
  imports: [
    SidebarComponent,
    StudioVideoSkeletonComponent,
    StudioVideoComponent,
    PaginationComponent,
  ],
})
export class Studio {
  readonly authStore = inject(AuthStore);
  private readonly videosService = inject(VideoService);
  private readonly creator = signal<Creator | null>(this.authStore.creator());

  protected readonly cursor = signal<string | undefined>(undefined);

  protected readonly videos = httpResource<SearchVideosResponse>(() =>
    this.videosService.searchVideos({
      channelId: this.creator()?.id,
      fields: 'contentDetails,statistics,thumbnails,channel,processingDetails',
      limit: 20,
      forMine: true,
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
