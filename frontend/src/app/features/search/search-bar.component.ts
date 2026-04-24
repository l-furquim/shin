import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe, UpperCasePipe } from '@angular/common';
import { Router } from '@angular/router';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardAvatarComponent } from '@/shared/components/avatar';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon/icon.component';
import { ZardInputDirective } from '@/shared/components/input';
import { ZardSkeletonComponent } from '@/shared/components/skeleton/skeleton.component';
import type { SearchVideoItem, SearchVideosResponse } from './search.types';
import { SearchService } from './search.service';

@Component({
  selector: 'app-search-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    UpperCasePipe,
    ZardInputDirective,
    ZardButtonComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardAlertComponent,
    ZardAvatarComponent,
    ZardSkeletonComponent,
  ],
  template: `
    <section class="space-y-5">
      <form class="space-y-3" (submit)="$event.preventDefault(); onSubmit()">
        <div class="flex gap-2">
          <div class="relative flex-1">
            <z-icon
              zType="search"
              zSize="sm"
              class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
            />
            <input
              z-input
              type="text"
              class="w-full pl-9"
              placeholder="Buscar por título, descrição ou canal..."
              [value]="q()"
              (input)="q.set(getInputValue($event))"
            />
          </div>

          <button z-button type="submit" [zLoading]="isLoading()">
            @if (!isLoading()) {
              <z-icon zType="search" zSize="sm" />
            }
            {{ isLoading() ? 'Buscando...' : 'Buscar' }}
          </button>

          <button z-button zType="outline" type="button" (click)="toggleFilters()">
            <z-icon zType="list-filter-plus" zSize="sm" />
            Filtros
            @if (activeFiltersCount() > 0) {
              <z-badge zType="default" class="ml-1 text-xs">{{ activeFiltersCount() }}</z-badge>
            }
          </button>

          @if (hasAnyInput()) {
            <button
              z-button
              zType="ghost"
              type="button"
              (click)="clearAll()"
              [zDisabled]="isLoading()"
            >
              <z-icon zType="x" zSize="sm" />
              Limpar
            </button>
          }
        </div>

        @if (showFilters()) {
          <div class="rounded-lg border bg-card p-4 space-y-4">
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
              <div class="space-y-1.5">
                <label class="text-xs font-medium text-muted-foreground">Idioma</label>
                <input
                  z-input
                  type="text"
                  class="w-full"
                  placeholder="pt, en, es..."
                  [value]="language()"
                  (input)="language.set(getInputValue($event))"
                />
              </div>

              <div class="space-y-1.5">
                <label class="text-xs font-medium text-muted-foreground">Categoria</label>
                <input
                  z-input
                  type="text"
                  class="w-full"
                  placeholder="Tecnologia, Música..."
                  [value]="category()"
                  (input)="category.set(getInputValue($event))"
                />
              </div>

              <div class="space-y-1.5">
                <label class="text-xs font-medium text-muted-foreground">Tags</label>
                <input
                  z-input
                  type="text"
                  class="w-full"
                  placeholder="tag1, tag2, tag3..."
                  [value]="tags()"
                  (input)="tags.set(getInputValue($event))"
                />
              </div>

              <div class="space-y-1.5">
                <label class="text-xs font-medium text-muted-foreground">Publicado de</label>
                <input
                  z-input
                  type="date"
                  class="w-full"
                  [value]="dateFrom()"
                  (input)="dateFrom.set(getInputValue($event))"
                />
              </div>

              <div class="space-y-1.5">
                <label class="text-xs font-medium text-muted-foreground">Publicado até</label>
                <input
                  z-input
                  type="date"
                  class="w-full"
                  [value]="dateTo()"
                  (input)="dateTo.set(getInputValue($event))"
                />
              </div>

              <div class="flex items-end pb-0.5">
                <label
                  class="flex h-10 w-full cursor-pointer items-center gap-2.5 rounded-md border px-3 text-sm hover:bg-accent/50 transition-colors"
                >
                  <input
                    type="checkbox"
                    class="accent-primary"
                    [checked]="forAdults()"
                    (change)="forAdults.set(getCheckboxValue($event))"
                  />
                  <span>Conteúdo adulto</span>
                </label>
              </div>
            </div>
          </div>
        }
      </form>

      @if (activeFilters().length > 0) {
        <div class="flex flex-wrap items-center gap-2">
          <span class="text-xs font-medium text-muted-foreground">Filtros ativos:</span>
          @for (filter of activeFilters(); track filter.label) {
            <span
              class="inline-flex items-center gap-1 rounded-full border bg-secondary px-2.5 py-0.5 text-xs font-medium"
            >
              {{ filter.label }}
              <button
                type="button"
                class="ml-0.5 rounded-full p-0.5 hover:bg-muted transition-colors leading-none"
                (click)="filter.clear()"
                [attr.aria-label]="'Remover filtro ' + filter.label"
              >
                <z-icon zType="x" zSize="sm" class="size-3" />
              </button>
            </span>
          }
        </div>
      }

      @if (errorMessage()) {
        <z-alert zType="destructive" [zDescription]="errorMessage()" />
      }

      @if (response()) {
        <div class="flex items-center justify-between gap-3">
          <p class="text-sm text-muted-foreground">{{ resultsCountLabel() }}</p>
          @if (hasNextPage()) {
            <z-badge zType="secondary">Mais resultados disponíveis</z-badge>
          }
        </div>

        @if (hasResults()) {
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for (video of response()!.results; track video.id) {
              <article
                class="group flex flex-col overflow-hidden rounded-xl border bg-card shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer"
                (click)="openVideo(video)"
                (keydown.enter)="openVideo(video)"
                tabindex="0"
                role="button"
              >
                <div class="relative aspect-video overflow-hidden bg-muted">
                  <img
                    [src]="video.thumbnailUrl"
                    [alt]="video.title"
                    class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                  />
                  @if (video.duration > 0) {
                    <span
                      class="absolute bottom-2 right-2 rounded bg-black/70 px-1.5 py-0.5 text-xs text-white font-mono tabular-nums"
                    >
                      {{ formatDuration(video.duration) }}
                    </span>
                  }
                </div>

                <div class="flex flex-1 flex-col gap-2 p-3">
                  <h3 class="line-clamp-2 text-sm font-semibold leading-snug">{{ video.title }}</h3>

                  <div class="flex items-center gap-2 text-sm text-muted-foreground">
                    <z-avatar
                      [zSrc]="video.channelAvatar"
                      [zFallback]="video.channelName?.[0] ?? '?'"
                      zSize="sm"
                    />
                    <span class="truncate">{{ video.channelName }}</span>
                  </div>

                  <div class="mt-auto flex items-center gap-3 text-xs text-muted-foreground pt-1">
                    @if (video.language) {
                      <span class="uppercase font-medium">{{ video.language }}</span>
                    }
                    @if (video.publishedAt) {
                      <span>{{ video.publishedAt | date: 'mediumDate' }}</span>
                    }
                  </div>
                </div>
              </article>
            }
          </div>
        } @else {
          <div class="flex flex-col items-center gap-3 py-16 text-center">
            <z-icon zType="search" zSize="xl" class="text-muted-foreground/40" />
            <p class="font-medium">Nenhum resultado encontrado.</p>
            <p class="text-xs text-muted-foreground">
              Tente termos diferentes ou remova alguns filtros.
            </p>
          </div>
        }

        @if (hasNextPage()) {
          <div class="flex justify-center pt-2">
            <button
              z-button
              zType="outline"
              type="button"
              (click)="loadNextPage()"
              [zLoading]="isLoading()"
            >
              Carregar mais
            </button>
          </div>
        }
      }

      @if (!response() && !isLoading() && !errorMessage()) {
        <div class="flex flex-col items-center gap-3 py-20 text-center text-muted-foreground">
          <z-icon zType="search" zSize="xl" class="opacity-20" />
          <p class="text-sm">Busque por vídeos, canais ou tópicos.</p>
        </div>
      }
    </section>
  `,
})
export class SearchBar {
  private readonly searchService = inject(SearchService);
  private readonly router = inject(Router);

  protected readonly q = signal('');
  protected readonly tags = signal('');
  protected readonly language = signal('');
  protected readonly category = signal('');
  protected readonly dateFrom = signal('');
  protected readonly dateTo = signal('');
  protected readonly forAdults = signal(false);
  protected readonly maxResults = signal(20);
  protected readonly pageToken = signal<string | undefined>(undefined);
  protected readonly showFilters = signal(false);

  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly response = signal<SearchVideosResponse | null>(null);

  protected readonly hasResults = computed(() => (this.response()?.results.length ?? 0) > 0);
  protected readonly hasNextPage = computed(() => Boolean(this.response()?.nextPageToken));

  protected readonly activeFilters = computed(() => {
    const filters: { label: string; clear: () => void }[] = [];
    if (this.language().trim()) {
      filters.push({
        label: `Idioma: ${this.language().trim()}`,
        clear: () => this.language.set(''),
      });
    }
    if (this.category().trim()) {
      filters.push({
        label: `Categoria: ${this.category().trim()}`,
        clear: () => this.category.set(''),
      });
    }
    if (this.tags().trim()) {
      filters.push({ label: `Tags: ${this.tags().trim()}`, clear: () => this.tags.set('') });
    }
    if (this.dateFrom() && this.dateTo()) {
      filters.push({
        label: `${this.dateFrom()} → ${this.dateTo()}`,
        clear: () => {
          this.dateFrom.set('');
          this.dateTo.set('');
        },
      });
    } else {
      if (this.dateFrom()) {
        filters.push({ label: `De: ${this.dateFrom()}`, clear: () => this.dateFrom.set('') });
      }
      if (this.dateTo()) {
        filters.push({ label: `Até: ${this.dateTo()}`, clear: () => this.dateTo.set('') });
      }
    }
    if (this.forAdults()) {
      filters.push({ label: 'Conteúdo adulto', clear: () => this.forAdults.set(false) });
    }
    return filters;
  });

  protected readonly activeFiltersCount = computed(() => this.activeFilters().length);

  protected readonly hasAnyInput = computed(
    () => this.q().trim() !== '' || this.activeFiltersCount() > 0 || this.response() !== null,
  );

  protected onSubmit(): void {
    this.pageToken.set(undefined);
    this.search(true);
  }

  protected loadNextPage(): void {
    const nextToken = this.response()?.nextPageToken;
    if (!nextToken || this.isLoading()) return;
    this.pageToken.set(nextToken);
    this.search(false);
  }

  protected toggleFilters(): void {
    this.showFilters.update((v) => !v);
  }

  protected clearAll(): void {
    this.q.set('');
    this.tags.set('');
    this.language.set('');
    this.category.set('');
    this.dateFrom.set('');
    this.dateTo.set('');
    this.forAdults.set(false);
    this.maxResults.set(20);
    this.pageToken.set(undefined);
    this.errorMessage.set('');
    this.response.set(null);
  }

  protected openVideo(video: SearchVideoItem): void {
    this.router.navigate(['/videos', video.id]);
  }

  protected getInputValue(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  protected getCheckboxValue(event: Event): boolean {
    return (event.target as HTMLInputElement).checked;
  }

  protected formatDuration(seconds: number): string {
    if (!seconds || seconds <= 0) return '';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);
    const mm = String(m).padStart(2, '0');
    const ss = String(s).padStart(2, '0');
    return h > 0 ? `${h}:${mm}:${ss}` : `${m}:${ss}`;
  }

  protected resultsCountLabel(): string {
    const res = this.response();
    if (!res) return '';
    const count = res.results.length;
    const total = res.pageInfo?.totalResults;
    if (typeof total === 'number' && total > count) {
      return `${count} de ${total} resultado(s)`;
    }
    return `${count} resultado(s)`;
  }

  private search(replaceResults: boolean): void {
    if (this.isLoading()) return;
    this.isLoading.set(true);
    this.errorMessage.set('');

    const tagList = this.tags()
      .split(',')
      .map((t) => t.trim())
      .filter(Boolean);

    this.searchService
      .searchVideos({
        q: this.q().trim() || undefined,
        tags: tagList.length ? tagList : undefined,
        language: this.language().trim() || undefined,
        category: this.category().trim() || undefined,
        dateFrom: this.dateFrom() || undefined,
        dateTo: this.dateTo() || undefined,
        forAdults: this.forAdults(),
        maxResults: this.maxResults(),
        pageToken: this.pageToken(),
      })
      .subscribe({
        next: (response) => {
          if (replaceResults || !this.response()) {
            this.response.set(response);
          } else {
            this.response.set({
              ...response,
              results: [...(this.response()?.results ?? []), ...response.results],
            });
          }
          this.isLoading.set(false);
        },
        error: (err: Error) => {
          this.errorMessage.set(err.message || 'Não foi possível realizar a busca.');
          this.isLoading.set(false);
        },
      });
  }
}
