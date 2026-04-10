import { ChangeDetectionStrategy, Component } from '@angular/core';

import { SidebarComponent } from '@/shared/components/sidebar/sidebar.component';

@Component({
  selector: 'app-explore-page',
  imports: [SidebarComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen md:flex">
      <app-sidebar></app-sidebar>
      <main class="w-full px-4 py-10 md:px-8">
        <div class="mx-auto w-full max-w-7xl space-y-3">
          <h1 class="text-3xl font-semibold tracking-tight md:text-4xl">Explore</h1>
          <p class="text-muted-foreground text-sm md:text-base">
            Descubra novos canais e videos. Esta pagina esta em construcao.
          </p>
        </div>
      </main>
    </div>
  `,
})
export class ExploreComponent {}
