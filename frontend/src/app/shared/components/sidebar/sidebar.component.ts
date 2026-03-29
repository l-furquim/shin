import { Component } from '@angular/core';
import { Search, SquareTerminal, Video } from 'lucide-angular';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  template: `
    <aside>
      <nav>
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
  imports: [RouterLink],
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
