import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { CreatorStore } from '@/core/stores/creator.store';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { UploadComponent } from '@/shared/components/upload/upload.component';
import { PlayerComponent } from '@/shared/components/player/player.component';

@Component({
  selector: 'app-dashboard-page',
  imports: [ZardBadgeComponent, UploadComponent, PlayerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main
      class="mx-auto flex min-h-screen w-full max-w-7xl flex-col gap-8 px-4 py-10 md:px-8 lg:gap-10"
    >
      <section class="space-y-3">
        <h1 class="text-3xl font-semibold tracking-tight md:text-4xl">
          Bem-vindo de volta, {{ creatorName() }}
        </h1>
      </section>

      <section class="grid gap-6 lg:grid-cols-2">
        <upload-area></upload-area>
        <video-player-area></video-player-area>
      </section>
    </main>
  `,
})
export class DashboardComponent {
  private readonly creatorStore = inject(CreatorStore);

  protected readonly creatorName = computed(() => {
    const creator = this.creatorStore.$creator();

    if (creator?.displayName?.trim()) {
      return creator.displayName;
    }

    return 'Criador';
  });
}
