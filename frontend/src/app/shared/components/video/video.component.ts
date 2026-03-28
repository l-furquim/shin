import type { VideoItem } from '@/features/videos/video.types';
import { Component } from '@angular/core';
import { PlayerComponent } from '../player/player.component';
import { CreatorAvatarComponent } from '../creator/creator-avatar.component';

@Component({
  selector: 'app-video',
  template: `
    <div class="flex w-full h-screen items-center justify-center">
      <div class="flex flex-col gap-3">
        <video-player-area [videoId]="this.video.id"></video-player-area>
        <app-creator-avatar [creator]="this.video.channel"></app-creator-avatar>
        <h1 class="text-xl font-bold text-left">{{ video.title }}</h1>
      </div>
    </div>
  `,
  imports: [PlayerComponent, CreatorAvatarComponent],
})
export class VideoComponent {
  video: VideoItem = history.state.video;
}
