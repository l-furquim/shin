import { Component, Input } from '@angular/core';
import { VideoItem } from '../videos/video.types';

@Component({
  selector: 'studio-video',
  template: '<h1>oie</h1>',
})
export class StudioVideoComponent {
  @Input({ required: true }) video!: VideoItem;
}
