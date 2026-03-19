import { Component } from '@angular/core';

@Component({
  selector: 'app-sidebar',
  template: `
    <aside>
      <nav>
        <ul>
          <li><a routerLink="/"></a></li>
        </ul>
      </nav>
    </aside>
  `,
})
export class SidebarComponent {}
