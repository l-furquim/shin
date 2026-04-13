import {
  Overlay,
  OverlayModule,
  OverlayPositionBuilder,
  type OverlayRef,
} from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { isPlatformBrowser } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  forwardRef,
  inject,
  type OnDestroy,
  PLATFORM_ID,
  signal,
  type TemplateRef,
  viewChild,
  ViewContainerRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { type ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { debounceTime, distinctUntilChanged, filter, Subject, switchMap } from 'rxjs';
import { TagService } from '@/features/tags/tag.service';
import type { Tag } from '@/features/tags/tag.types';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardIconComponent } from '@/shared/components/icon';

type OnTouchedType = () => void;
type OnChangeType = (value: string[]) => void;

@Component({
  selector: 'z-tag-input',
  imports: [OverlayModule, ZardBadgeComponent, ZardIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TagInputComponent),
      multi: true,
    },
  ],
  template: `
    <div
      class="flex min-h-10 w-full cursor-text flex-wrap items-center gap-1.5 rounded-lg border bg-transparent px-3 py-2 text-sm transition-colors focus-within:ring-2 focus-within:ring-ring/50 focus-within:border-ring"
      (click)="focusInput()"
    >
      @for (name of selected(); track name) {
        <z-badge zShape="pill" zType="secondary" class="flex items-center gap-1 pr-1">
          <span>{{ name }}</span>
          <button
            type="button"
            class="ml-0.5 rounded-full p-0.5 opacity-60 transition-opacity hover:opacity-100 focus:outline-none"
            (click)="$event.stopPropagation(); remove(name)"
          >
            <z-icon zType="x" zSize="sm" />
          </button>
        </z-badge>
      }
      <input
        #searchInput
        type="text"
        class="min-w-[120px] flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
        placeholder="{{ selected().length ? '' : 'Buscar ou criar tags...' }}"
        [value]="query()"
        (focus)="onFocus()"
        (input)="onInput($event)"
        (keydown)="onKeydown($event)"
      />
    </div>

    <ng-template #dropdownTpl>
      <div
        role="listbox"
        class="z-50 rounded-xl border bg-popover shadow-md outline-none"
        (keydown.escape)="close()"
      >
        <div class="p-1 space-y-0.5 max-h-60 overflow-y-auto">
          @if (isLoading()) {
            <div class="flex items-center gap-2 px-2 py-1.5 text-xs text-muted-foreground">
              <z-icon zType="loader-circle" zSize="sm" class="animate-spin" />
              Buscando...
            </div>
          }

          @for (tag of results(); track tag.id) {
            <button
              type="button"
              role="option"
              class="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-sm transition-colors hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:text-accent-foreground focus:outline-none"
              [attr.aria-selected]="isSelected(tag.name)"
              (click)="toggle(tag.name)"
            >
              <z-icon
                [zType]="isSelected(tag.name) ? 'check' : 'tag'"
                zSize="sm"
                [class]="isSelected(tag.name) ? 'text-primary' : 'text-muted-foreground'"
              />
              <span class="flex-1 text-left">{{ tag.name }}</span>
            </button>
          }

          @if (canCreate()) {
            <button
              type="button"
              class="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-sm text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground focus:outline-none"
              (click)="createAndAdd()"
            >
              <z-icon zType="plus" zSize="sm" />
              Criar "{{ query() }}"
            </button>
          }

          @if (results().length === 0 && !isLoading() && !canCreate()) {
            <p class="px-2 py-1.5 text-xs text-muted-foreground">Nenhuma tag encontrada.</p>
          }
        </div>
      </div>
    </ng-template>
  `,
})
export class TagInputComponent implements ControlValueAccessor, OnDestroy {
  private readonly tagService = inject(TagService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly overlay = inject(Overlay);
  private readonly overlayPositionBuilder = inject(OverlayPositionBuilder);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private readonly platformId = inject(PLATFORM_ID);

  readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');
  readonly dropdownTpl = viewChild.required<TemplateRef<void>>('dropdownTpl');

  private overlayRef?: OverlayRef;
  private portal?: TemplatePortal;
  private readonly querySubject = new Subject<string>();
  private onChange: OnChangeType = () => {};
  private onTouched: OnTouchedType = () => {};

  readonly query = signal('');
  readonly results = signal<Tag[]>([]);
  readonly selected = signal<string[]>([]);
  readonly isOpen = signal(false);
  readonly isLoading = signal(false);

  readonly canCreate = computed(() => {
    const q = this.query().trim();
    if (!q || q.length < 2) return false;
    return !this.results().some((t) => t.name.toLowerCase() === q.toLowerCase());
  });

  constructor() {
    this.querySubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => {
          this.isLoading.set(true);
          return this.tagService.searchTags(q);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.results.set(res.items);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false),
      });
  }

  ngOnDestroy(): void {
    this.destroyOverlay();
  }

  onFocus(): void {
    this.open();
  }

  onInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.query.set(value);
    this.querySubject.next(value);
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      const q = this.query().trim();
      if (!q) return;
      const exact = this.results().find((t) => t.name.toLowerCase() === q.toLowerCase());
      if (exact) {
        this.toggle(exact.name);
      } else if (this.canCreate()) {
        this.createAndAdd();
      }
    } else if (event.key === 'Escape') {
      this.close();
    } else if (event.key === 'Backspace' && !this.query() && this.selected().length > 0) {
      const last = this.selected()[this.selected().length - 1];
      this.remove(last);
    }
  }

  focusInput(): void {
    this.searchInput()?.nativeElement.focus();
  }

  isSelected(name: string): boolean {
    return this.selected().includes(name);
  }

  toggle(name: string): void {
    if (this.isSelected(name)) {
      this.remove(name);
    } else {
      this.add(name);
    }
  }

  remove(name: string): void {
    this.selected.update((s) => s.filter((n) => n !== name));
    this.emitChange();
  }

  add(name: string): void {
    if (!this.isSelected(name)) {
      this.selected.update((s) => [...s, name]);
      this.clearInput();
      this.emitChange();
    }
  }

  createAndAdd(): void {
    const name = this.query().trim().toLowerCase();
    if (!name) return;
    this.tagService.createTag(name).subscribe({
      next: (tag) => {
        this.results.update((r) => [tag, ...r.filter((t) => t.name !== tag.name)]);
        this.add(tag.name);
      },
    });
  }

  writeValue(value: string[] | null): void {
    this.selected.set(value ?? []);
  }

  registerOnChange(fn: OnChangeType): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: OnTouchedType): void {
    this.onTouched = fn;
  }

  setDisabledState(): void {}

  private open(): void {
    if (this.isOpen()) return;

    if (!this.overlayRef) {
      this.createOverlay();
    }
    if (!this.overlayRef) return;

    if (this.overlayRef.hasAttached()) {
      this.overlayRef.detach();
    }

    this.portal = new TemplatePortal(this.dropdownTpl(), this.viewContainerRef);
    this.overlayRef.attach(this.portal);
    this.overlayRef.updateSize({ width: this.elementRef.nativeElement.offsetWidth });
    this.isOpen.set(true);

    if (this.results().length === 0) {
      this.querySubject.next('');
    }
  }

  close(): void {
    if (this.overlayRef?.hasAttached()) {
      this.overlayRef.detach();
    }
    this.isOpen.set(false);
    this.onTouched();
  }

  private clearInput(): void {
    this.query.set('');
    const inputEl = this.searchInput()?.nativeElement;
    if (inputEl) inputEl.value = '';
    this.querySubject.next('');
  }

  private emitChange(): void {
    this.onChange(this.selected());
  }

  private createOverlay(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    try {
      const positionStrategy = this.overlayPositionBuilder
        .flexibleConnectedTo(this.elementRef)
        .withPositions([
          { originX: 'center', originY: 'bottom', overlayX: 'center', overlayY: 'top', offsetY: 4 },
          {
            originX: 'center',
            originY: 'top',
            overlayX: 'center',
            overlayY: 'bottom',
            offsetY: -4,
          },
        ])
        .withPush(false);

      this.overlayRef = this.overlay.create({
        positionStrategy,
        hasBackdrop: false,
        scrollStrategy: this.overlay.scrollStrategies.reposition(),
        maxHeight: 320,
      });

      this.overlayRef
        .outsidePointerEvents()
        .pipe(
          filter((event) => !this.elementRef.nativeElement.contains(event.target as Node)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe(() => this.close());
    } catch {
      // overlay creation failed (e.g. SSR)
    }
  }

  private destroyOverlay(): void {
    this.overlayRef?.dispose();
    this.overlayRef = undefined;
  }
}
