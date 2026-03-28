import { Component, inject, Input, OnInit } from '@angular/core';
import { ZardCardComponent } from '../card';
import type { VideoItem } from '@/features/videos/video.types';
import { Router } from '@angular/router';

@Component({
  selector: 'video-card',
  template: `
    <z-card (click)="onClick()" class="w-sm flex flex-col hover:cursor-pointer">
      <img
        class="rounded-md"
        [src]="video.thumbnails.get('small')?.url"
        alt="{{ video.title }}"
        [width]="350"
        [height]="200"
      />
      <h3 class="font-semibold text-md pt-3">{{ video.title }}</h3>
      <div
        class="flex items-center gap-2 pt-4 text-sm hover:cursor-pointer hover:text-gray-60"
        (click)="onChannelClick()"
      >
        <img class="rounded-full w-9 h-8" [src]="video.thumbnails.get('small')?.url" />
        <p>Nome do canal</p>
      </div>
      <div class="flex items-center gap-2 pt-4 text-sm">
        <p>{{ video.statistics?.viewCount ?? 0 }} views</p>
        <p>Published 5 hours ago</p>
      </div>
    </z-card>
  `,
  imports: [ZardCardComponent],
})
export class VideoCard implements OnInit {
  @Input() video!: VideoItem;
  private readonly router = inject(Router);

  ngOnInit() {
    console.log(this.video);
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
