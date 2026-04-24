import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SidebarComponent } from '@/shared/components/sidebar/sidebar.component';
import { SearchBar } from '../search/search-bar.component';

@Component({
  selector: 'app-explore-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SidebarComponent, SearchBar],
  template: `
    <div class="min-h-screen md:flex">
      <app-sidebar></app-sidebar>
      <main class="w-full px-4 py-10 md:px-8">
        <div class="mx-auto flex w-full max-w-7xl flex-col gap-8">
          <section class="space-y-1">
            <h1 class="text-3xl font-semibold tracking-tight md:text-4xl">Explorar</h1>
            <p class="text-muted-foreground text-sm">Descubra vídeos e canais.</p>
          </section>
          <app-search-bar />
        </div>
      </main>
    </div>
  `,
})
export class ExploreComponent {}
