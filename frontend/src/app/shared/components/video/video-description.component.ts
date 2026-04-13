import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';

@Component({
  selector: 'video-description',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="rounded-xl bg-stone-100 p-4 text-sm leading-relaxed">
      <p class="whitespace-pre-wrap" [class.line-clamp-3]="!expanded()">
        {{ description() }}
      </p>
      @if (!expanded()) {
        <button class="mt-2 font-semibold hover:underline" (click)="expanded.set(true)">
          Ver mais
        </button>
      } @else {
        <button class="mt-2 font-semibold hover:underline" (click)="expanded.set(false)">
          Ver menos
        </button>
      }
    </div>
  `,
})
export class VideoDescriptionComponent {
  readonly description = input('');

  protected readonly expanded = signal(false);
}
