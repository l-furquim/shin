import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { SidebarComponent } from '@/shared/components/sidebar/sidebar.component';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { ZardIconComponent } from '@/shared/components/icon/icon.component';
import { ZardSkeletonComponent } from '@/shared/components/skeleton/skeleton.component';
import { VideoCard } from '@/shared/components/video/video-card.component';
import { VideoCardSkeletonComponent } from '@/shared/components/video/video-card-skeleton.component';
import { PaginationComponent } from '@/shared/components/pagination/pagination.component';
import { SubscribedChannelCardComponent } from './subscribed-channel-card.component';
import { SubscriptionService } from './subscription.service';
import type { SearchVideosResponse } from '../videos/video.types';
import type { SubscriptionsResponse } from './subscription.types';

@Component({
  selector: 'app-channels-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    SidebarComponent,
    ZardButtonComponent,
    ZardIconComponent,
    ZardSkeletonComponent,
    VideoCard,
    VideoCardSkeletonComponent,
    PaginationComponent,
    SubscribedChannelCardComponent,
  ],
  template: `
    <div class="min-h-screen md:flex">
      <app-sidebar></app-sidebar>
      <main class="w-full px-4 py-10 md:px-8">
        <div class="mx-auto flex w-full max-w-7xl flex-col gap-8">

          <section class="space-y-3">
            <h1 class="text-3xl font-semibold tracking-tight md:text-4xl">Channels</h1>
          </section>

          <div class="flex gap-2 border-b pb-2">
            <button
              z-button
              [zType]="activeTab() === 'videos' ? 'default' : 'ghost'"
              (click)="activeTab.set('videos')"
            >
              <z-icon zType="film" zSize="sm" />
              Vídeos
            </button>
            <button
              z-button
              [zType]="activeTab() === 'channels' ? 'default' : 'ghost'"
              (click)="activeTab.set('channels')"
            >
              <z-icon zType="users" zSize="sm" />
              Inscrições
            </button>
          </div>

          @if (activeTab() === 'videos') {
            <section class="w-full grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
              @if (feedVideos.isLoading()) {
                @for (item of [].constructor(9); track $index) {
                  <video-card-skeleton></video-card-skeleton>
                }
              }
              @if (feedVideos.error()) {
                <p class="text-destructive col-span-3">Erro ao carregar vídeos.</p>
              }
              @if (feedVideos.hasValue()) {
                @for (video of feedVideos.value().items; track video.id) {
                  <video-card [video]="video" />
                } @empty {
                  <p class="col-span-3 text-muted-foreground text-center py-12">
                    Inscreva-se em canais para ver os vídeos deles aqui.
                  </p>
                }
              }
            </section>
            @if (feedVideos.hasValue()) {
              <app-pagination
                [nextPageToken]="feedVideos.value().nextPageToken || null"
                [prevPageToken]="feedVideos.value().prevPageToken || null"
                [loading]="feedVideos.isLoading()"
                (next)="onVideosNext($event)"
                (prev)="onVideosPrev($event)"
              />
            }
          }

          @if (activeTab() === 'channels') {
            <section class="flex flex-col gap-3">
              @if (subscribedChannels.isLoading()) {
                @for (item of [].constructor(6); track $index) {
                  <div class="flex items-center gap-4 rounded-lg border p-4">
                    <z-skeleton class="size-14 rounded-full shrink-0" />
                    <div class="flex flex-1 flex-col gap-2">
                      <z-skeleton class="h-4 w-1/3" />
                      <z-skeleton class="h-3 w-1/5" />
                    </div>
                    <z-skeleton class="h-8 w-36 shrink-0" />
                  </div>
                }
              }
              @if (subscribedChannels.error()) {
                <p class="text-destructive">Erro ao carregar inscrições.</p>
              }
              @if (subscribedChannels.hasValue()) {
                @for (channel of subscribedChannels.value().items; track channel.channelId) {
                  <app-subscribed-channel-card
                    [channel]="channel"
                    (unsubscribe)="onChannelUnsubscribed()"
                  />
                } @empty {
                  <p class="text-muted-foreground text-center py-12">
                    Você ainda não se inscreveu em nenhum canal.
                  </p>
                }
              }
            </section>
            @if (subscribedChannels.hasValue()) {
              <app-pagination
                [nextPageToken]="subscribedChannels.value().nextPageToken || null"
                [prevPageToken]="subscribedChannels.value().prevPageToken || null"
                [loading]="subscribedChannels.isLoading()"
                (next)="onChannelsNext($event)"
                (prev)="onChannelsPrev($event)"
              />
            }
          }

        </div>
      </main>
    </div>
  `,
})
export class ChannelsComponent {
  private readonly subscriptionService = inject(SubscriptionService);

  protected readonly activeTab = signal<'videos' | 'channels'>('videos');

  protected readonly videosCursor = signal<string | undefined>(undefined);
  protected readonly feedVideos = httpResource<SearchVideosResponse>(() =>
    this.subscriptionService.getSubscriptionFeed(this.videosCursor()),
  );

  protected readonly channelsCursor = signal<string | undefined>(undefined);
  protected readonly subscribedChannels = httpResource<SubscriptionsResponse>(() =>
    this.subscriptionService.getSubscribedChannels(this.channelsCursor()),
  );

  protected onChannelUnsubscribed(): void {
    this.channelsCursor.set(undefined);
  }

  protected onVideosNext(token: string): void { this.videosCursor.set(token); }
  protected onVideosPrev(token: string): void { this.videosCursor.set(token); }
  protected onChannelsNext(token: string): void { this.channelsCursor.set(token); }
  protected onChannelsPrev(token: string): void { this.channelsCursor.set(token); }
}
