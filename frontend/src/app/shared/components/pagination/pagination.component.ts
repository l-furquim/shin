import { ChangeDetectionStrategy, Component, EventEmitter, input, Output } from '@angular/core';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { ZardIconComponent } from '@/shared/components/icon/icon.component';

@Component({
  selector: 'app-pagination',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ZardButtonComponent, ZardIconComponent],
  template: `
    <div class="flex items-center justify-center gap-3 pt-4">
      <button
        z-button
        zType="outline"
        zSize="default"
        [zDisabled]="!prevPageToken() || loading()"
        (click)="prevPageToken() && prev.emit(prevPageToken()!)"
      >
        <z-icon zType="chevron-left" zSize="sm" />
        Anterior
      </button>

      <button
        z-button
        zType="outline"
        zSize="default"
        [zDisabled]="!nextPageToken() || loading()"
        (click)="nextPageToken() && next.emit(nextPageToken()!)"
      >
        Próxima
        <z-icon zType="chevron-right" zSize="sm" />
      </button>
    </div>
  `,
})
export class PaginationComponent {
  readonly nextPageToken = input<string | null>(null);
  readonly prevPageToken = input<string | null>(null);
  readonly loading = input<boolean>(false);

  @Output() next = new EventEmitter<string>();
  @Output() prev = new EventEmitter<string>();
}
