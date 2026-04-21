import { Component, Input } from '@angular/core';
import { VideoItem } from '../videos/video.types';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { VisibilityPipe } from '@/shared/pipes/visibility.pipe';

@Component({
  selector: 'studio-video',
  template: `
    <a [href]="'/videos/' + this.video.id + '/manage'">
      <div class="flex flex-col sm:flex-row items-start gap-4">
        <img
          [src]="this.video.thumbnails['maxres'].url"
          alt="Thumbnail do vídeo"
          class="w-full sm:w-48 rounded-lg border object-cover aspect-video"
        />
        <div class="space-y-2">
          <p class=" font-medium">{{ this.video.title }}</p>
          <p class="text-xs text-muted-foreground">Em desenvolvimento.</p>
        </div>
        <z-badge>{{ this.video.visibility | visibilityPipe }}</z-badge>
      </div>
    </a>
  `,
  imports: [ZardBadgeComponent, VisibilityPipe],
})
export class StudioVideoComponent {
  @Input({ required: true }) video!: VideoItem;
}
