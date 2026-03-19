import type { Creator } from '@/features/creator/creator.types';
import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class CreatorStore {
  private readonly storageKey = 'shin.creator';
  private creator = signal<Creator | null>(null);

  $creator = this.creator.asReadonly();

  constructor() {
    this.hydrateFromStorage();
  }

  setCreator(creator: Creator): void {
    this.creator.set(creator);
    this.persist(creator);
  }

  clear() {
    this.creator.set(null);
    this.removeFromStorage();
  }

  getCreator(): Creator | null {
    return this.creator();
  }

  private hydrateFromStorage(): void {
    if (typeof window === 'undefined') {
      return;
    }

    const serialized = window.localStorage.getItem(this.storageKey);
    if (!serialized) {
      return;
    }

    try {
      const raw = JSON.parse(serialized) as Partial<Creator>;
      if (!raw.id || !raw.email || !raw.displayName) {
        this.removeFromStorage();
        return;
      }

      this.creator.set({
        id: raw.id,
        displayName: raw.displayName,
        email: raw.email,
        showAdultContent: Boolean(raw.showAdultContent),
        locale: raw.locale ?? 'pt-BR',
        avatar: raw.avatar ?? '',
        banner: raw.banner ?? '',
        deviceId: raw.deviceId ?? '',
        updatedAt: raw.updatedAt ? new Date(raw.updatedAt) : new Date(),
        createdAt: raw.createdAt ? new Date(raw.createdAt) : new Date(),
      });
    } catch {
      this.removeFromStorage();
    }
  }

  private persist(creator: Creator): void {
    if (typeof window === 'undefined') {
      return;
    }

    window.localStorage.setItem(this.storageKey, JSON.stringify(creator));
  }

  private removeFromStorage(): void {
    if (typeof window === 'undefined') {
      return;
    }

    window.localStorage.removeItem(this.storageKey);
  }
}
