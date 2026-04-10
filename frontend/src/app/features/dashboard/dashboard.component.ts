import { ChangeDetectionStrategy, Component, computed, inject, PLATFORM_ID } from '@angular/core';
import { AuthStore } from '@/core/stores/auth.store';
import { VideoService } from '../videos/video.service';
import type { SearchVideosResponse } from '../videos/video.types';
import { httpResource } from '@angular/common/http';
import { VideoCard } from '@/shared/components/video/video-card.component';
import { VideoCardSkeletonComponent } from '@/shared/components/video/video-card-skeleton.component';
import { isPlatformBrowser } from '@angular/common';
import { SidebarComponent } from '@/shared/components/sidebar/sidebar.component';

@Component({
  selector: 'app-dashboard-page',
  imports: [VideoCard, VideoCardSkeletonComponent, SidebarComponent],
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

          <section class="grid gap-6 lg:grid-cols-2">
            @if (!this.useApi) {
              @for (video of this.VIDEOS_MOCK.items; track video.id) {
                <video-card [video]="video"></video-card>
              }
            } @else {
              @if (videos.isLoading()) {
                @for (item of [].constructor(20); track $index) {
                  <video-card-skeleton></video-card-skeleton>
                }
              }
              @if (videos.error()) {
                <p>{{ videos.error() }}</p>
              }
              @if (videos.hasValue()) {
                @for (video of videos.value().items; track video.id) {
                  <video-card [video]="video"></video-card>
                }
              }
            }
          </section>
        </div>
      </main>
    </div>
  `,
})
export class DashboardComponent {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly authStore = inject(AuthStore);
  private readonly videosService = inject(VideoService);

  protected readonly useApi = true;
  private readonly isBrowser = isPlatformBrowser(this.platformId);
  protected readonly VIDEOS_MOCK: SearchVideosResponse = {
    nextPageToken: '',
    prevPageToken: '',
    pageInfo: {
      totalResults: 0,
      resultsPerPage: 20,
    },
    items: [
      {
        id: 'oie',
        visibility: 'PUBLIC',
        categoryId: '',
        title: 'Meu video poggers',
        description: 'Gente da like ai deixa o scrito',
        channel: {
          avatarUrl: '',
          name: 'Meu canal',
          id: 'id',
        },
        likedByMe: false,
        tags: new Set(),
        publishedAt: new Date(),
        scheduledPublishAt: new Date(),
        createdAt: new Date(),
        thumbnails: new Map([
          [
            'small',
            {
              url: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ4M7nZjgoZAoxHYQi_kKvCnWceK3LWl92uXQ&s',
              width: 120,
              height: 90,
            },
          ],
        ]),
        statistics: {
          viewCount: 1200,
          likeCount: 10,
          commentCount: 9,
        },
      },
    ],
  };

  protected readonly videos = httpResource<SearchVideosResponse>(() => {
    if (!this.isBrowser || this.useApi) {
      return undefined;
    }

    return this.videosService.searchVideos({
      fields: 'contentDetails,statistics,thumbnails',
      limit: 20,
    });
  });

  protected readonly creatorName = computed(() => {
    const creator = this.authStore.creator();

    if (creator?.displayName?.trim()) {
      return creator.displayName;
    }

    return 'Criador';
  });
}
