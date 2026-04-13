import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { catchError, of, switchMap } from 'rxjs';
import { CommentService } from '@/features/comments/comment.service';
import type { CommentDto } from '@/features/comments/comment.types';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardAvatarComponent } from '@/shared/components/avatar';

@Component({
  selector: 'video-comments',
  imports: [FormsModule, DatePipe, ZardButtonComponent, ZardIconComponent, ZardAvatarComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="space-y-5">
      <h2 class="text-base font-semibold">{{ comments().length }} comentários</h2>

      @if (userId()) {
        <div class="flex gap-3">
          <z-avatar [zSrc]="''" [zFallback]="userInitials()" zSize="sm" class="shrink-0" />
          <div class="flex flex-1 flex-col gap-2">
            <textarea
              [(ngModel)]="newText"
              rows="2"
              placeholder="Adicione um comentário..."
              class="w-full resize-none rounded-lg border bg-transparent px-3 py-2 text-sm outline-none focus:border-stone-400 focus:ring-0"
            ></textarea>
            <div class="flex justify-end gap-2">
              <button
                type="button"
                class="rounded-full px-4 py-1.5 text-sm font-medium hover:bg-stone-100"
                (click)="newText = ''"
              >
                Cancelar
              </button>
              <z-button
                zType="default"
                [zDisabled]="!newText.trim() || submitting()"
                [zLoading]="submitting()"
                (click)="submit()"
              >
                Comentar
              </z-button>
            </div>
          </div>
        </div>
      }

      @for (comment of comments(); track comment.id) {
        <div class="flex gap-3">
          <z-avatar
            [zSrc]="'https://' + comment.authorAvatarUrl"
            [zFallback]="initials(comment)"
            zSize="sm"
            class="shrink-0"
          />
          <div class="flex-1 space-y-1">
            <div class="flex items-center gap-2">
              <span class="text-sm font-semibold">
                {{ comment.authorDisplayName ?? comment.authorId }}
              </span>
              <span class="text-muted-foreground text-xs">
                {{ comment.createdAt | date: 'dd/MM/y' }}
              </span>
            </div>
            <p class="text-sm leading-relaxed">{{ comment.textDisplay }}</p>
            <div class="flex items-center gap-3 text-xs text-muted-foreground">
              <button type="button" class="flex items-center gap-1 hover:text-foreground">
                <z-icon zType="thumbs-up" zSize="sm" />
                {{ comment.likeCount }}
              </button>
              <button type="button" class="hover:text-foreground">Responder</button>
              @if (comment.authorId === userId()) {
                <button
                  type="button"
                  class="hover:text-destructive"
                  (click)="deleteComment(comment.id)"
                >
                  Excluir
                </button>
              }
            </div>
          </div>
        </div>
      }

      @if (loading()) {
        <div class="flex justify-center py-4">
          <z-icon zType="loader-circle" class="animate-spin text-muted-foreground" />
        </div>
      }

      @if (!loading() && comments().length === 0 && !error()) {
        <p class="py-4 text-center text-sm text-muted-foreground">Seja o primeiro a comentar.</p>
      }

      @if (error()) {
        <p class="py-2 text-sm text-muted-foreground">Não foi possível carregar comentários.</p>
      }
    </section>
  `,
})
export class VideoCommentsComponent implements OnInit {
  private readonly commentService = inject(CommentService);
  private readonly destroyRef = inject(DestroyRef);

  readonly videoId = input.required<string>();
  readonly userId = input<string | null>(null);

  protected readonly comments = signal<CommentDto[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly submitting = signal(false);

  protected newText = '';

  ngOnInit(): void {
    this.loadComments();
  }

  protected userInitials(): string {
    return (this.userId() ?? '?').slice(0, 2).toUpperCase();
  }

  protected initials(comment: CommentDto): string {
    return (comment.authorDisplayName ?? comment.authorId).slice(0, 2).toUpperCase();
  }

  protected submit(): void {
    const text = this.newText.trim();
    if (!text) return;

    this.submitting.set(true);
    this.commentService
      .createComment({ videoId: this.videoId(), channelId: this.userId() ?? '', content: text })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of(null)),
      )
      .subscribe((comment) => {
        this.submitting.set(false);
        if (comment) {
          this.newText = '';
          this.comments.update((list) => [comment, ...list]);
        }
      });
  }

  protected deleteComment(commentId: string): void {
    this.commentService
      .deleteComment(commentId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of(null)),
      )
      .subscribe(() => {
        this.comments.update((list) => list.filter((c) => c.id !== commentId));
      });
  }

  private loadComments(): void {
    this.loading.set(true);
    this.error.set(false);

    this.commentService
      .listThreads(this.videoId())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        switchMap((threads) => {
          if (!threads.items.length) return of(null);
          const ids = threads.items.map((t) => t.id);
          return this.commentService.listComments({ ids, maxResults: ids.length });
        }),
        catchError(() => of(null)),
      )
      .subscribe((res) => {
        this.loading.set(false);
        if (res === null) {
          this.error.set(true);
          return;
        }
        if (res) {
          this.comments.set(res.items);
        }
      });
  }
}
