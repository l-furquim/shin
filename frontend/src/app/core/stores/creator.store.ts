import type { Creator } from '@/features/creator/creator.types';
import { signal } from '@angular/core';

export class CreatorStore {
  private creator = signal<Creator | null>(null);

  $creator = this.creator.asReadonly();

  setCreator(creator: Creator): void {
    this.creator.set(creator);
  }

  clear() {
    this.creator.set(null);
  }

  getCreator(): Creator | null {
    return this.creator();
  }
}
