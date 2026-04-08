import { Component } from '@angular/core';
import { Search, SquareTerminal, Video } from 'lucide-angular';
import { RouterLink } from '@angular/router';
import { ZardAvatarComponent } from '../avatar';
import { ZardButtonComponent } from '../button';

@Component({
  selector: 'app-sidebar',
  template: `
    <aside>
      <nav class="w-xs bg-sidebar p-2">
        <z-button [zType]="'outline'" class="card flex gap-2 ites-center">
          <z-avatar zSrc="https://github.com/shadcn.png" zFallback="ZA" zSize="sm" />
          Lucas Hernandes
        </z-button>
        <ul>
          @for (item of sidebarItems; track item.id) {
            <li [routerLink]="item.url">
              <span>lucas</span>
            </li>
          }
        </ul>
      </nav>
    </aside>
  `,
  imports: [RouterLink, ZardAvatarComponent, ZardButtonComponent],
})
export class SidebarComponent {
  readonly sidebarItems = [
    {
      id: 'feed',
      label: 'Feed',
      icon: SquareTerminal,
      url: '',
    },
    {
      id: 'explore',
      label: 'Explore',
      icon: Search,
      url: '/explore',
    },
    {
      id: 'my-channels',
      label: 'My Channels',
      icon: Video,
      url: '/my-channels',
    },
    {
      id: 'me',
      label: 'Me',
      icon: null,
      url: '/me',
    },
  ];
}
