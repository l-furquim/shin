import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { ZardCardComponent } from '../card';
import type { Thumbnail, VideoItem } from '@/features/videos/video.types';
import { Router } from '@angular/router';
import { ZardAvatarComponent } from '../avatar';

@Component({
  selector: 'video-card',
  template: `
    <z-card (click)="onClick()" class="w-xs flex bg-none shadow-none flex-col hover:cursor-pointer">
      <img
        class="rounded-md"
        [src]="this.thumbResolution()?.url"
        alt="{{ video.title }}"
        [width]="this.thumbResolution()?.width"
        [height]="this.thumbResolution()?.height"
      />
      <h3 class="font-semibold text-md pt-3">{{ video.title }}</h3>
      <div
        class="flex items-center gap-2 pt-4 text-sm hover:cursor-pointer hover:text-gray-60"
        (click)="onChannelClick()"
      >
        <z-avatar [zSrc]="video.channel.avatarUrl"></z-avatar>
        <p>{{ video.channel.name }}</p>
      </div>
      <div class="flex items-center gap-2 pt-4 text-sm">
        <p>{{ video.statistics?.viewCount ?? 0 }} views</p>
        <p>Published 5 hours ago</p>
      </div>
    </z-card>
  `,
  imports: [ZardCardComponent, ZardAvatarComponent],
})
export class VideoCard implements OnInit {
  @Input() video!: VideoItem;

  private readonly router = inject(Router);
  readonly thumbResolution = signal<Thumbnail | null>(null);

  ngOnInit() {
    console.log(this.video);
    this.thumbResolution.set(this.video.thumbnails['maxres']);
  }

  async onClick() {
    await this.router.navigate(['/videos', this.video.id], {
      state: { video: this.video },
    });
  }

  onChannelClick() {
    this.router.navigate(['/channels', this.video.channel?.id]);
  }
}
