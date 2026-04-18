import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { ZardAvatarComponent } from '../avatar';
import { ZardButtonComponent } from '../button';
import { ZardIconComponent, type ZardIcon } from '../icon';
import { AuthStore } from '@/core/stores/auth.store';

@Component({
  selector: 'app-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <aside
      class="w-full border-b border-sidebar-border bg-sidebar md:h-screen md:w-64 md:border-r md:border-b-0"
    >
      <nav class="flex h-full flex-col p-3 md:p-4">
        <div class="mb-4 px-2">
          <p class="text-xs font-medium tracking-wider text-sidebar-foreground/60 uppercase">
            Navegacao
          </p>
        </div>

        <ul class="space-y-1">
          @for (item of sidebarItems; track item.id) {
            <li>
              <a
                z-button
                zType="ghost"
                zSize="default"
                [routerLink]="item.url"
                routerLinkActive="bg-sidebar-accent text-sidebar-accent-foreground"
                [routerLinkActiveOptions]="item.exact ? exactMatchOptions : subsetMatchOptions"
                class="w-full justify-start gap-2.5 border border-transparent text-sidebar-foreground/80 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
              >
                <z-icon [zType]="item.icon" zSize="sm" />
                <span>{{ item.label }}</span>
              </a>
            </li>
          }
        </ul>

        <div class="mt-4 border-t border-sidebar-border pt-4">
          <a
            z-button
            zType="default"
            zSize="default"
            routerLink="/upload"
            class="w-full justify-start gap-2.5"
          >
            <z-icon zType="cloud-upload" zSize="sm" />
            <span>Upload</span>
          </a>
        </div>

        <div class="mt-auto pt-4">
          <button
            z-button
            zType="ghost"
            zSize="lg"
            class="w-full justify-start gap-2 text-sidebar-foreground"
          >
            <z-avatar [zSrc]="this.authStore.creator()?.avatar ?? ''" zFallback="LH" zSize="sm" />
            <span class="truncate">{{ this.authStore.creator()?.displayName }}</span>
          </button>
        </div>
      </nav>
    </aside>
  `,
  imports: [
    RouterLink,
    RouterLinkActive,
    ZardAvatarComponent,
    ZardButtonComponent,
    ZardIconComponent,
  ],
})
export class SidebarComponent {
  protected readonly authStore = inject(AuthStore);
  protected readonly exactMatchOptions = { exact: true };
  protected readonly subsetMatchOptions = { exact: false };

  readonly sidebarItems = [
    {
      id: 'home',
      label: 'Home',
      icon: 'house' as ZardIcon,
      url: '/',
      exact: true,
    },
    {
      id: 'explore',
      label: 'Explore',
      icon: 'search' as ZardIcon,
      url: '/explore',
      exact: true,
    },
    {
      id: 'channels',
      label: 'Channels',
      icon: 'users' as ZardIcon,
      url: '/channels',
      exact: false,
    },
    {
      id: 'studio',
      label: 'Studio',
      icon: 'layout-dashboard' as ZardIcon,
      url: '/studio',
      exact: true,
    },
  ];
}
