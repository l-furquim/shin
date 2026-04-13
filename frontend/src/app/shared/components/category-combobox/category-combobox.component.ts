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
import { CategoryService } from '@/features/videos/category.service';
import type { Category } from '@/features/videos/category.types';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardInputDirective } from '@/shared/components/input';

type OnTouchedType = () => void;
type OnChangeType = (value: number | null) => void;

@Component({
  selector: 'z-category-combobox',
  imports: [OverlayModule, ZardIconComponent, ZardInputDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CategoryComboboxComponent),
      multi: true,
    },
  ],
  template: `
    <button
      type="button"
      role="combobox"
      [attr.aria-expanded]="isOpen()"
      class="flex h-10 w-full items-center justify-between rounded-lg border bg-transparent px-3 text-sm transition-colors hover:bg-accent/50 focus:outline-none focus:ring-2 focus:ring-ring/50 focus:border-ring"
      (click)="toggle()"
    >
      <span [class]="selectedCategory() ? 'text-foreground' : 'text-muted-foreground'">
        {{ selectedCategory()?.name ?? 'Selecionar categoria...' }}
      </span>
      <z-icon zType="chevron-down" zSize="lg" class="opacity-50 shrink-0" />
    </button>

    <ng-template #dropdownTpl>
      <div
        class="z-50 rounded-xl border bg-popover shadow-md outline-none"
        (keydown.escape)="close()"
      >
        <div class="border-b px-3 py-2">
          <input
            #searchInput
            z-input
            type="text"
            placeholder="Buscar categoria..."
            class="w-full border-0 bg-transparent px-0 py-0 text-sm shadow-none focus:ring-0 focus-visible:ring-0"
            [value]="query()"
            (input)="onInput($event)"
            (keydown.escape)="close()"
          />
        </div>
        <div role="listbox" class="max-h-60 overflow-y-auto p-1 space-y-0.5">
          @if (isLoading()) {
            <div class="flex items-center gap-2 px-2 py-1.5 text-xs text-muted-foreground">
              <z-icon zType="loader-circle" zSize="sm" class="animate-spin" />
              Buscando...
            </div>
          }

          @for (cat of results(); track cat.id) {
            <button
              type="button"
              role="option"
              class="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-sm transition-colors hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:text-accent-foreground focus:outline-none"
              [attr.aria-selected]="selectedCategory()?.id === cat.id"
              (click)="select(cat)"
            >
              <z-icon
                [zType]="selectedCategory()?.id === cat.id ? 'check' : 'folder'"
                zSize="sm"
                [class]="
                  selectedCategory()?.id === cat.id ? 'text-primary' : 'text-muted-foreground'
                "
              />
              <span class="flex-1 text-left">{{ cat.name }}</span>
            </button>
          }

          @if (results().length === 0 && !isLoading()) {
            <p class="px-2 py-1.5 text-xs text-muted-foreground">Nenhuma categoria encontrada.</p>
          }
        </div>
      </div>
    </ng-template>
  `,
})
export class CategoryComboboxComponent implements ControlValueAccessor, OnDestroy {
  private readonly categoryService = inject(CategoryService);
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
  readonly results = signal<Category[]>([]);
  readonly selectedCategory = signal<Category | null>(null);
  readonly isOpen = signal(false);
  readonly isLoading = signal(false);

  constructor() {
    this.querySubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => {
          this.isLoading.set(true);
          return this.categoryService.searchCategories(q);
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

  toggle(): void {
    if (this.isOpen()) {
      this.close();
    } else {
      this.open();
    }
  }

  onInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.query.set(value);
    this.querySubject.next(value);
  }

  select(cat: Category): void {
    this.selectedCategory.set(cat);
    this.onChange(cat.id);
    this.close();
  }

  writeValue(value: number | null): void {
    if (value == null) {
      this.selectedCategory.set(null);
      return;
    }
    const existing = this.results().find((c) => c.id === value);
    if (existing) {
      this.selectedCategory.set(existing);
    }
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

    setTimeout(() => this.searchInput()?.nativeElement.focus(), 0);
  }

  close(): void {
    if (this.overlayRef?.hasAttached()) {
      this.overlayRef.detach();
    }
    this.isOpen.set(false);
    this.query.set('');
    this.onTouched();
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
        maxHeight: 380,
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
