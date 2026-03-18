import { AuthService } from '@/features/auth/auth.service';
import { TokenService } from '@/features/auth/token.service';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, filter, Observable, switchMap, take, throwError } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshSubject = new BehaviorSubject<string | null>(null);

  constructor(
    private router: Router,
    private tokenService: TokenService,
    private authService: AuthService,
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const accessToken = this.tokenService.getAccessToken();

    const authReq = accessToken
      ? req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } })
      : req;

    return next.handle(req).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status === 401) {
          return this.handleUnauthorized(req, next);
        }
        return throwError(() => err);
      }),
    );
  }

  private handleUnauthorized(req: HttpRequest<any>, next: HttpHandler) {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshSubject.next(null);

      return this.authService.refreshToken().pipe(
        switchMap((response: any) => {
          this.isRefreshing = false;

          const newAccessToken = response.access_token;
          this.tokenService.setAccessToken(newAccessToken);

          this.refreshSubject.next(newAccessToken);

          return next.handle(
            req.clone({ setHeaders: { Authorization: `Bearer ${newAccessToken}` } }),
          );
        }),
        catchError((err) => {
          this.isRefreshing = false;
          this.tokenService.clear();

          // Usuario sem auth ligado
          this.router.navigate(['/login']);
          return throwError(() => new Error('Unauthorized'));
        }),
      );
    }

    // espera o refresh atual finalizar.
    return this.refreshSubject.pipe(
      filter((token) => token !== null),
      take(1),
      switchMap((token) => {
        return next.handle(
          req.clone({
            setHeaders: { Authorization: `Bearer ${token}` },
          }),
        );
      }),
    );
  }
}
