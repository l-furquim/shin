import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { ZardAvatarComponent } from '@/shared/components/avatar';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { CompactNumberPipe } from '@/shared/pipes/compact-number.pipe';
import { SubscriptionService } from './subscription.service';
import type { SubscribedChannel } from './subscription.types';

@Component({
  selector: 'app-subscribed-channel-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ZardAvatarComponent, ZardButtonComponent, CompactNumberPipe],
  template: `
    <div class="flex items-center gap-4 rounded-lg border p-4">
      <z-avatar
        [zSrc]="channel().avatarUrl"
        [zFallback]="channel().name[0]"
        zSize="lg"
      />
      <div class="flex flex-1 flex-col gap-0.5 min-w-0">
        <p class="font-medium truncate">{{ channel().name }}</p>
        <p class="text-sm text-muted-foreground">
          {{ channel().subscribersCount | compactNumber }} inscritos
        </p>
      </div>
      <button
        z-button
        zType="outline"
        zSize="sm"
        [zLoading]="unsubscribing()"
        (click)="onUnsubscribe()"
      >
        Cancelar inscrição
      </button>
    </div>
  `,
})
export class SubscribedChannelCardComponent {
  private readonly subscriptionService = inject(SubscriptionService);

  readonly channel = input.required<SubscribedChannel>();
  readonly unsubscribe = output<void>();

  protected readonly unsubscribing = signal(false);

  protected onUnsubscribe(): void {
    this.unsubscribing.set(true);
    this.subscriptionService.unsubscribe(this.channel().channelId).subscribe({
      next: () => {
        this.unsubscribing.set(false);
        this.unsubscribe.emit();
      },
      error: () => {
        this.unsubscribing.set(false);
      },
    });
  }
}
