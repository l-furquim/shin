import { isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AuthStore } from '@/core/stores/auth.store';
import { AuthService } from '@/features/auth/auth.service';

@Injectable({ providedIn: 'root' })
export class SessionBootstrapService {
  private readonly authStore = inject(AuthStore);
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);

  async initialize(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    if (this.authStore.isAuthenticated()) {
      return;
    }

    const refreshed = await firstValueFrom(this.authService.refreshToken());
    if (refreshed?.token) {
      this.authStore.setAccessToken(refreshed.token);
    }
  }
}
