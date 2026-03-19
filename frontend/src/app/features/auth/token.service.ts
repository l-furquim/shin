import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly storageKey = 'shin.access-token';
  private accessToken: string | null = null;

  constructor() {
    if (typeof window !== 'undefined') {
      this.accessToken = window.localStorage.getItem(this.storageKey);
    }
  }

  setAccessToken(token: string): void {
    this.accessToken = token;

    if (typeof window !== 'undefined') {
      window.localStorage.setItem(this.storageKey, token);
    }
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  clear(): void {
    this.accessToken = null;

    if (typeof window !== 'undefined') {
      window.localStorage.removeItem(this.storageKey);
    }
  }
}
