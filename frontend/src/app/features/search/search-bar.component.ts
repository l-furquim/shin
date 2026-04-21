import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardInputDirective } from '@/shared/components/input';
import type { SearchVideoItem, SearchVideosResponse } from './search.types';
import { SearchService } from './search.service';

@Component({
  selector: 'app-search-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ZardInputDirective,
    ZardButtonComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardAlertComponent,
  ],
  template: `
    <section class="space-y-5">
      <form class="grid grid-cols-1 gap-3 md:grid-cols-12" (ngSubmit)="onSubmit()">
        <div class="md:col-span-5">
          <input
            z-input
            type="text"
            class="w-full"
            placeholder="Buscar por título, descrição ou canal"
            [value]="q()"
            (input)="q.set(getInputValue($event))"
          />
        </div>

        <!-- <div class="md:col-span-3">
          <input
            z-input
            type="text"
            class="w-full"
            placeholder="Tags separadas por vírgula"
            [value]="tags()"
            (input)="tags.set(getInputValue($event))"
          />
        </div>

        <div class="md:col-span-2">
          <input
            z-input
            type="text"
            class="w-full"
            placeholder="Idioma (pt, en...)"
            [value]="language()"
            (input)="language.set(getInputValue($event))"
          />
        </div>

        <div class="md:col-span-2">
          <input
            z-input
            type="text"
            class="w-full"
            placeholder="Categoria"
            [value]="category()"
            (input)="category.set(getInputValue($event))"
          />
        </div>

        <div class="md:col-span-2">
          <input
            z-input
            type="date"
            class="w-full"
            [value]="dateFrom()"
            (input)="dateFrom.set(getInputValue($event))"
          />
        </div>

        <div class="md:col-span-2">
          <input
            z-input
            type="date"
            class="w-full"
            [value]="dateTo()"
            (input)="dateTo.set(getInputValue($event))"
          />
        </div>

        <div class="md:col-span-2">
          <input
            z-input
            type="number"
            min="1"
            max="50"
            class="w-full"
            [value]="maxResultsValue()"
            (input)="onMaxResultsInput($event)"
          />
        </div>

        <label class="md:col-span-2 flex h-10 items-center gap-2 rounded-lg border px-3 text-sm">
          <input
            type="checkbox"
            [checked]="forAdults()"
            (change)="forAdults.set(getCheckboxValue($event))"
          />
          Adulto
        </label> -->

        <div class="md:col-span-4 flex gap-2">
          <z-button class="w-full" [zLoading]="isLoading()" type="submit">
            @if (!isLoading()) {
              <z-icon zType="search" zSize="sm" />
            }
            {{ isLoading() ? 'Buscando...' : 'Buscar' }}
          </z-button>

          <z-button
            zType="outline"
            type="button"
            (click)="clearFilters()"
            [zDisabled]="isLoading()"
          >
            Limpar
          </z-button>
        </div>
      </form>

      @if (errorMessage()) {
        <z-alert zType="destructive" [zDescription]="errorMessage()" />
      }

      @if (response()) {
        <div class="flex items-center justify-between gap-3">
          <p class="text-sm text-muted-foreground">
            {{ resultsCountLabel() }}
          </p>
          @if (hasNextPage()) {
            <z-badge zType="secondary">Mais resultados disponíveis</z-badge>
          }
        </div>

        @if (hasResults()) {
          <div class="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
            @for (video of response()!.results; track video.id) {
              <article class="rounded-xl border p-3 space-y-2">
                <img
                  [src]="video.thumbnailUrl"
                  [alt]="video.title"
                  class="h-40 w-full rounded-md object-cover"
                />
                <h3 class="line-clamp-2 text-sm font-semibold">{{ video.title }}</h3>
                <p class="line-clamp-2 text-xs text-muted-foreground">{{ video.description }}</p>
                <div class="text-xs text-muted-foreground">
                  <span>{{ video.channelName }}</span>
                  <span> • </span>
                  <span>{{ video.language || '—' }}</span>
                </div>
                <a
                  z-button
                  zType="outline"
                  class="w-full"
                  [href]="video.videoLink"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Ver vídeo
                </a>
              </article>
            }
          </div>
        } @else {
          <p class="text-sm text-muted-foreground">Nenhum resultado encontrado.</p>
        }

        @if (hasNextPage()) {
          <div class="flex justify-center pt-2">
            <z-button zType="outline" (click)="loadNextPage()" [zLoading]="isLoading()">
              Carregar mais
            </z-button>
          </div>
        }
      }
    </section>
  `,
})
export class SearchBar {
  private readonly searchService = inject(SearchService);

  protected readonly q = signal('');
  protected readonly tags = signal('');
  protected readonly language = signal('');
  protected readonly category = signal('');
  protected readonly dateFrom = signal('');
  protected readonly dateTo = signal('');
  protected readonly forAdults = signal(false);
  protected readonly maxResults = signal(20);
  protected readonly pageToken = signal<string | undefined>(undefined);

  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly response = signal<SearchVideosResponse | null>(null);

  protected readonly hasResults = computed(() => (this.response()?.results.length ?? 0) > 0);
  protected readonly hasNextPage = computed(() => Boolean(this.response()?.nextPageToken));
  protected readonly maxResultsValue = computed(() => `${this.maxResults()}`);

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

  protected clearFilters(): void {
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

  protected getInputValue(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  protected getCheckboxValue(event: Event): boolean {
    return (event.target as HTMLInputElement).checked;
  }

  protected onMaxResultsInput(event: Event): void {
    const raw = Number((event.target as HTMLInputElement).value);
    if (Number.isNaN(raw)) {
      this.maxResults.set(20);
      return;
    }

    this.maxResults.set(Math.min(Math.max(Math.trunc(raw), 1), 50));
  }

  protected resultsCountLabel(): string {
    const res = this.response();
    if (!res) return '';
    const count = res.results.length;
    const total = res.pageInfo?.totalResults;
    if (typeof total === 'number') {
      return `${count} resultado(s) nesta página de ${total} total`;
    }
    return `${count} resultado(s)`;
  }

  private search(replaceResults: boolean): void {
    if (this.isLoading()) return;

    this.isLoading.set(true);
    this.errorMessage.set('');

    const tagList = this.tags()
      .split(',')
      .map((tag) => tag.trim())
      .filter((tag) => Boolean(tag));

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
              results: [...this.response()!.results, ...response.results],
            });
          }
          this.isLoading.set(false);
        },
        error: (error: Error) => {
          this.errorMessage.set(error.message || 'Não foi possível realizar a busca.');
          this.isLoading.set(false);
        },
      });
  }
}
