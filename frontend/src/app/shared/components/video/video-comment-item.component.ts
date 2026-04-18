import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { catchError, of } from 'rxjs';
import { CommentService } from '@/features/comments/comment.service';
import type { CommentDto } from '@/features/comments/comment.types';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardAvatarComponent } from '@/shared/components/avatar';
import { SafeHtmlPipe } from '@/shared/pipes/safe-html.pipe';
import { Creator } from '@/features/creator/creator.types';

@Component({
  selector: 'video-comment-item',
  imports: [
    FormsModule,
    DatePipe,
    ZardButtonComponent,
    ZardIconComponent,
    ZardAvatarComponent,
    SafeHtmlPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex gap-3">
      <z-avatar
        [zSrc]="comment().authorAvatarUrl ?? ''"
        [zFallback]="initials()"
        zSize="sm"
        class="shrink-0"
      />
      <div class="flex-1 space-y-1">
        <div class="flex items-center gap-2">
          <span class="text-sm font-semibold">
            {{ comment().authorDisplayName ?? comment().authorId }}
          </span>
          <span class="text-muted-foreground text-xs">
            {{ comment().createdAt | date: 'dd/MM/y' }}
          </span>
        </div>

        <p
          class="text-sm leading-relaxed [&_a]:text-blue-500 [&_a]:underline [&_a]:hover:text-blue-400"
          [innerHTML]="comment().textDisplay | safeHtml"
        ></p>

        <div class="flex items-center gap-3 text-xs text-muted-foreground">
          <button type="button" class="flex items-center gap-1 hover:text-foreground">
            <z-icon zType="thumbs-up" zSize="sm" />
            {{ comment().likeCount }}
          </button>

          @if (user()) {
            <button type="button" class="hover:text-foreground" (click)="toggleReplyForm()">
              Responder
            </button>
          }

          @if (comment().authorId === user()?.id) {
            <button type="button" class="hover:text-destructive" (click)="onDelete()">
              Excluir
            </button>
          }
        </div>

        @if (showReplyForm()) {
          <div class="flex gap-3 pt-2">
            <z-avatar [zSrc]="user()?.avatar ?? ''" zSize="sm" class="shrink-0" />
            <div class="flex flex-1 flex-col gap-2">
              <textarea
                [ngModel]="replyText()"
                (ngModelChange)="replyText.set($event)"
                rows="2"
                placeholder="Adicione uma resposta..."
                class="w-full resize-none rounded-lg border bg-transparent px-3 py-2 text-sm outline-none focus:border-stone-400 focus:ring-0"
              ></textarea>
              <div class="flex justify-end gap-2">
                <button
                  type="button"
                  class="rounded-full px-4 py-1.5 text-sm font-medium hover:bg-stone-100"
                  (click)="cancelReply()"
                >
                  Cancelar
                </button>
                <z-button
                  zType="default"
                  [zDisabled]="!replyText().trim() || submitting()"
                  [zLoading]="submitting()"
                  (click)="submitReply()"
                >
                  Responder
                </z-button>
              </div>
            </div>
          </div>
        }

        @if (comment().parentId === null) {
          <button
            type="button"
            class="flex items-center gap-1 pt-1 text-xs font-medium text-blue-500 hover:text-blue-400"
            (click)="toggleReplies()"
          >
            <z-icon [zType]="showReplies() ? 'chevron-up' : 'chevron-down'" zSize="sm" />
            @if (showReplies()) {
              Ocultar respostas
            } @else if (replyCount() > 0) {
              Ver {{ replyCount() }} {{ replyCount() === 1 ? 'resposta' : 'respostas' }}
            } @else {
              Ver respostas
            }
          </button>
        }

        @if (showReplies()) {
          <div class="mt-3 space-y-4 pt-5">
            @if (repliesLoading()) {
              <div class="flex justify-center py-2">
                <z-icon zType="loader-circle" class="animate-spin text-muted-foreground" />
              </div>
            }
            @for (reply of replies(); track reply.id) {
              <video-comment-item
                [comment]="reply"
                [user]="user()"
                (deleted)="onReplyDeleted($event)"
              />
            }
          </div>
        }
      </div>
    </div>
  `,
})
export class VideoCommentItemComponent {
  private readonly commentService = inject(CommentService);
  private readonly destroyRef = inject(DestroyRef);

  readonly comment = input.required<CommentDto>();
  readonly replyCount = input(0);
  readonly user = input<Creator | null>(null);

  readonly deleted = output<string>();

  protected readonly replies = signal<CommentDto[]>([]);
  protected readonly showReplies = signal(false);
  protected readonly repliesLoading = signal(false);
  protected readonly repliesLoaded = signal(false);
  protected readonly showReplyForm = signal(false);
  protected readonly replyText = signal('');
  protected readonly submitting = signal(false);

  protected initials(): string {
    return (this.comment().authorDisplayName ?? this.comment().authorId).slice(0, 2).toUpperCase();
  }

  protected toggleReplyForm(): void {
    this.showReplyForm.update((v) => !v);
    if (!this.showReplyForm()) {
      this.replyText.set('');
    }
  }

  protected cancelReply(): void {
    this.showReplyForm.set(false);
    this.replyText.set('');
  }

  protected submitReply(): void {
    const text = this.replyText().trim();
    if (!text || !this.user()) return;

    this.submitting.set(true);
    this.commentService
      .createComment({
        videoId: this.comment().videoId,
        channelId: this.user()!.id,
        content: text,
        parentId: this.comment().id,
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of(null)),
      )
      .subscribe((reply) => {
        this.submitting.set(false);
        if (reply) {
          this.replyText.set('');
          this.showReplyForm.set(false);
          this.replies.update((list) => [...list, reply]);
          if (!this.showReplies()) {
            this.showReplies.set(true);
          }
        }
      });
  }

  protected toggleReplies(): void {
    if (!this.showReplies()) {
      this.showReplies.set(true);
      if (!this.repliesLoaded()) {
        this.loadReplies();
      }
    } else {
      this.showReplies.set(false);
    }
  }

  protected onDelete(): void {
    this.commentService
      .deleteComment(this.comment().id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of(null)),
      )
      .subscribe(() => {
        this.deleted.emit(this.comment().id);
      });
  }

  protected onReplyDeleted(replyId: string): void {
    this.replies.update((list) => list.filter((r) => r.id !== replyId));
  }

  private loadReplies(): void {
    this.repliesLoading.set(true);
    this.commentService
      .listComments({ parentId: this.comment().id, maxResults: 100 })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of(null)),
      )
      .subscribe((res) => {
        this.repliesLoading.set(false);
        this.repliesLoaded.set(true);
        if (res) {
          this.replies.set(res.items);
        }
      });
  }
}
