import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  Input,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { catchError, of, switchMap } from 'rxjs';
import { CommentService } from '@/features/comments/comment.service';
import type { CommentDto, ThreadDto } from '@/features/comments/comment.types';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardAvatarComponent } from '@/shared/components/avatar';
import { VideoCommentItemComponent } from './video-comment-item.component';
import { Creator } from '@/features/creator/creator.types';

@Component({
  selector: 'video-comments',
  imports: [FormsModule, ZardButtonComponent, ZardIconComponent, ZardAvatarComponent, VideoCommentItemComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="space-y-5">
      <h2 class="text-base font-semibold">{{ comments().length }} comentários</h2>

      @if (this.user) {
        <div class="flex gap-3">
          <z-avatar [zSrc]="this.user.avatar" zSize="sm" class="shrink-0" />
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
        <video-comment-item
          [comment]="comment"
          [replyCount]="threads().get(comment.id)?.totalReplyCount ?? 0"
          [user]="user"
          (deleted)="onCommentDeleted($event)"
        />
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

  @Input() user!: Creator | null;

  protected readonly comments = signal<CommentDto[]>([]);
  protected readonly threads = signal<Map<string, ThreadDto>>(new Map());
  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly submitting = signal(false);

  protected newText = '';

  ngOnInit(): void {
    this.loadComments();
  }

  protected submit(): void {
    const text = this.newText.trim();
    if (!text) return;

    this.submitting.set(true);
    this.commentService
      .createComment({ videoId: this.videoId(), channelId: this.user?.id ?? '', content: text })
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

  protected onCommentDeleted(commentId: string): void {
    this.comments.update((list) => list.filter((c) => c.id !== commentId));
  }

  private loadComments(): void {
    this.loading.set(true);
    this.error.set(false);

    this.commentService
      .listThreads(this.videoId())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        switchMap((threadRes) => {
          if (!threadRes.items.length) return of(null);
          const map = new Map(threadRes.items.map((t) => [t.id, t]));
          this.threads.set(map);
          const ids = threadRes.items.map((t) => t.id);
          return this.commentService.listComments({ ids, maxResults: ids.length });
        }),
        catchError(() => of(null)),
      )
      .subscribe((res) => {
        this.loading.set(false);
        if (res === null) {
          if (!this.threads().size) {
            this.error.set(false);
          } else {
            this.error.set(true);
          }
          return;
        }
        if (res) {
          this.comments.set(res.items);
        }
      });
  }
}
