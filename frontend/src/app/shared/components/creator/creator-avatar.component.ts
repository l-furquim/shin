import { ChannelDetails } from '@/features/videos/video.types';
import { Input } from '@angular/core';
import { Component } from '@angular/core';

@Component({
  selector: 'app-creator-avatar',
  template: `
    <img
      class="rounded-full w-9 h-8"
      [src]="this.creator.avatarUrl"
      alt="{{ this.creator.name }}"
    />

    <p>{{ this.creator.name }}</p>
  `,
})
export class CreatorAvatarComponent {
  @Input() creator!: ChannelDetails;
}
