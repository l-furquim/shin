import { Component } from '@angular/core';
import { ZardSkeletonComponent } from '@/shared/components/skeleton/skeleton.component';

@Component({
  selector: 'studio-video-skeleton',
  imports: [ZardSkeletonComponent],
  template: `
    <div class="flex items-center gap-4 rounded-lg border p-3">
      <z-skeleton class="w-36 shrink-0 rounded-md aspect-video" />
      <div class="flex flex-1 flex-col gap-2">
        <z-skeleton class="h-4 w-3/4" />
        <z-skeleton class="h-4 w-1/4" />
      </div>
      <div class="hidden md:flex items-center gap-6">
        <z-skeleton class="h-4 w-10" />
        <z-skeleton class="h-4 w-10" />
        <z-skeleton class="h-4 w-10" />
      </div>
      <z-skeleton class="hidden lg:block h-4 w-20" />
    </div>
  `,
})
export class StudioVideoSkeletonComponent {}
