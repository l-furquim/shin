import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AuthStore } from '@/core/stores/auth.store';
import { VideoService } from '../videos/video.service';
import type { SearchVideosResponse } from '../videos/video.types';
import { httpResource } from '@angular/common/http';
import { VideoCard } from '@/shared/components/video/video-card.component';
import { VideoCardSkeletonComponent } from '@/shared/components/video/video-card-skeleton.component';
import { SidebarComponent } from '@/shared/components/sidebar/sidebar.component';
import { PaginationComponent } from '@/shared/components/pagination/pagination.component';
@Component({
  selector: 'app-dashboard-page',
  imports: [
    VideoCard,
    VideoCardSkeletonComponent,
    SidebarComponent,
    PaginationComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen md:flex">
      <app-sidebar></app-sidebar>
      <main class="w-full px-4 py-10 md:px-8">
        <div class="mx-auto flex w-full max-w-7xl flex-col gap-8 lg:gap-10">
          <section class="space-y-3">
            <h1 class="text-3xl font-semibold tracking-tight md:text-4xl">
              Bem-vindo de volta, {{ creatorName() }}
            </h1>
          </section>

          <section class="w-full grid gap-3 grid-cols-3">
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
                <video-card [video]="video"></video-card>
              } @empty {
                <p class="text-muted-foreground col-span-2">Nenhum vídeo encontrado.</p>
              }
            }
          </section>

          @if (videos.hasValue()) {
            <app-pagination
              [nextPageToken]="videos.value().nextPageToken || null"
              [prevPageToken]="videos.value().prevPageToken || null"
              [loading]="videos.isLoading()"
              (next)="onNext($event)"
              (prev)="onPrev($event)"
            />
          }
        </div>
      </main>
    </div>
  `,
})
export class DashboardComponent {
  private readonly authStore = inject(AuthStore);
  private readonly videosService = inject(VideoService);

  protected readonly cursor = signal<string | undefined>(undefined);

  protected readonly videos = httpResource<SearchVideosResponse>(() =>
    this.videosService.searchVideos({
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

  protected readonly creatorName = computed(() => {
    const creator = this.authStore.creator();
    return creator?.displayName?.trim() || 'Criador';
  });
}
