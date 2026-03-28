import { Component } from '@angular/core';

import { ZardSkeletonComponent } from '../skeleton';

@Component({
  selector: 'video-card-skeleton',
  imports: [ZardSkeletonComponent],
  standalone: true,
  template: `
    <div class="flex flex-col space-y-3">
      <z-skeleton class="h-[125px] w-[250px] rounded-xl" />
      <div class="space-y-2">
        <z-skeleton class="h-4 w-[250px]" />
        <z-skeleton class="h-4 w-[200px]" />
      </div>
    </div>
  `,
})
export class VideoCardSkeletonComponent {}
