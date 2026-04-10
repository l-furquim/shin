import { AuthStore } from '@/core/stores/auth.store';
import { AuthService } from '@/features/auth/auth.service';
import { AuthResponse } from '@/features/auth/auth.types';
import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, filter, switchMap, take, throwError } from 'rxjs';

function addToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

function handle401(
  req: HttpRequest<unknown>,
  next: Parameters<HttpInterceptorFn>[1],
  authStore: AuthStore,
  authService: AuthService,
  router: Router,
) {
  if (!authStore.isRefreshing) {
    authStore.isRefreshing = true;
    authStore.refreshing$.next(null);

    return authService.refreshToken().pipe(
      switchMap((response: AuthResponse | null) => {
        authStore.isRefreshing = false;

        if (!response) {
          authStore.clearSession();
          router.navigate(['/login']);
          return throwError(() => new Error('Unauthorized'));
        }

        authStore.setAccessToken(response.token);
        authStore.refreshing$.next(response.token);

        return next(addToken(req, response.token));
      }),
      catchError((err) => {
        authStore.isRefreshing = false;
        authStore.clearSession();
        router.navigate(['/login']);
        return throwError(() => err);
      }),
    );
  }

  return authStore.refreshing$.pipe(
    filter((token): token is string => token !== null),
    take(1),
    switchMap((token) => next(addToken(req, token))),
  );
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.includes('amazonaws.com')) {
    return next(req);
  }

  const authStore = inject(AuthStore);
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authStore.accessToken();
  const authReq = token ? addToken(req, token) : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status !== 401) return throwError(() => err);
      return handle401(req, next, authStore, authService, router);
    }),
  );
};
