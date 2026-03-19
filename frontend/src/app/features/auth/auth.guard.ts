import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { CreatorStore } from '@/core/stores/creator.store';
import { TokenService } from '@/features/auth/token.service';

export const authGuard: CanActivateFn = () => {
  const tokenService = inject(TokenService);
  const creatorStore = inject(CreatorStore);
  const router = inject(Router);

  const hasToken = !!tokenService.getAccessToken();
  const hasCreator = !!creatorStore.$creator();

  if (hasToken && hasCreator) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
