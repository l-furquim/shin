import { VideoVisibility } from '@/features/videos/video.types';
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'visibilityPipe', standalone: true })
export class VisibilityPipe implements PipeTransform {
  transform(value: VideoVisibility): string {
    if (value === 'PUBLIC') {
      return 'Public';
    } else if (value === 'PRIVATE') {
      return 'Private';
    } else {
      return 'Not listed';
    }
  }
}
