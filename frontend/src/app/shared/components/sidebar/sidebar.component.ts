import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { ZardAvatarComponent } from '../avatar';
import { ZardButtonComponent } from '../button';
import { ZardIconComponent, type ZardIcon } from '../icon';

@Component({
  selector: 'app-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <aside class="w-full border-b border-sidebar-border bg-sidebar md:h-screen md:w-64 md:border-r md:border-b-0">
      <nav class="flex h-full flex-col p-3 md:p-4">
        <div class="mb-4 px-2">
          <p class="text-xs font-medium tracking-wider text-sidebar-foreground/60 uppercase">Navegacao</p>
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

        <div class="mt-auto pt-4">
          <button
            z-button
            zType="outline"
            zSize="lg"
            class="w-full justify-start gap-2 border-sidebar-border bg-background/60 text-sidebar-foreground"
          >
            <z-avatar zSrc="https://github.com/shadcn.png" zFallback="LH" zSize="sm" />
            <span class="truncate">Lucas Hernandes</span>
          </button>
        </div>
      </nav>
    </aside>
  `,
  imports: [RouterLink, RouterLinkActive, ZardAvatarComponent, ZardButtonComponent, ZardIconComponent],
})
export class SidebarComponent {
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
      id: 'dashboard',
      label: 'Dashboard',
      icon: 'layout-dashboard' as ZardIcon,
      url: '/dashboard',
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
  ];
}
