import type { Creator } from '@/features/creator/creator.types';
import { computed, Injectable, signal } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly STORAGE_KEY = 'shin.creator';

  private readonly _accessToken = signal<string | null>(null);
  private readonly _creator = signal<Creator | null>(null);

  readonly accessToken = this._accessToken.asReadonly();
  readonly creator = this._creator.asReadonly();
  readonly isAuthenticated = computed(() => !!this._accessToken());

  readonly refreshing$ = new BehaviorSubject<string | null>(null);
  isRefreshing = false;

  constructor() {
    this.hydrateCreator();
  }

  setSession(token: string, creator: Creator): void {
    this._accessToken.set(token);
    this._creator.set(creator);
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(this.STORAGE_KEY, JSON.stringify(creator));
    }
  }

  setAccessToken(token: string): void {
    this._accessToken.set(token);
  }

  clearSession(): void {
    this._accessToken.set(null);
    this._creator.set(null);
    if (typeof window !== 'undefined') {
      window.localStorage.removeItem(this.STORAGE_KEY);
    }
  }

  private hydrateCreator(): void {
    if (typeof window === 'undefined') return;

    const serialized = window.localStorage.getItem(this.STORAGE_KEY);
    if (!serialized) return;

    try {
      const raw = JSON.parse(serialized) as Partial<Creator>;
      if (!raw.id || !raw.email || !raw.displayName) {
        window.localStorage.removeItem(this.STORAGE_KEY);
        return;
      }

      this._creator.set({
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
      window.localStorage.removeItem(this.STORAGE_KEY);
    }
  }
}
