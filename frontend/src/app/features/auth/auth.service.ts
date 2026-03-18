import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of } from 'rxjs';
import type { AuthRequest, AuthResponse, RegisterRequest, RegisterResponse } from './auth.types';

@Injectable()
export class AuthService {
  private http = inject(HttpClient);

  register(request: RegisterRequest): Observable<RegisterResponse | null> {
    return this.http.post<RegisterResponse>('/auth/register', request).pipe(
      catchError((err) => {
        console.log('erro', err);
        return of(null);
      }),
    );
  }

  auth(request: AuthRequest): Observable<AuthResponse | null> {
    return this.http.post<AuthResponse>('/auth', request).pipe(
      catchError((err) => {
        console.log('erro', err);
        return of(null);
      }),
    );
  }

  refreshToken(): Observable<AuthResponse | null> {
    return this.http.post<AuthResponse>('/auth/refresh', {}).pipe(
      catchError((err) => {
        console.log('erro refresh', err);
        return of(null);
      }),
    );
  }
}
