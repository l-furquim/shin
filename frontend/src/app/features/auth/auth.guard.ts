import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { AuthStore } from '@/core/stores/auth.store';
import { AuthService } from '@/features/auth/auth.service';

export const authGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const authService = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  if (authStore.isAuthenticated()) return true;

  return authService.refreshToken().pipe(
    map((res) => {
      if (!res) return router.createUrlTree(['/login']);
      authStore.setAccessToken(res.token);
      return true;
    }),
    catchError(() => of(router.createUrlTree(['/login']))),
  );
};
