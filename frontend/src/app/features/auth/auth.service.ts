import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of } from 'rxjs';
import type {
  AuthRequest,
  AuthResponse,
  CreatorSignUpRequest,
  CreatorSignUpResponse,
  MeResponse,
  RegisterRequest,
  RegisterResponse,
} from './auth.types';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  register(request: RegisterRequest): Observable<RegisterResponse | null> {
    return this.http.post<RegisterResponse>('/api/v1/auth/register', request).pipe(
      catchError(() => of(null)),
    );
  }

  createCreator(request: CreatorSignUpRequest): Observable<CreatorSignUpResponse | null> {
    const formData = new FormData();
    formData.append(
      'data',
      new Blob([JSON.stringify(request)], { type: 'application/json' }),
    );

    return this.http
      .post<CreatorSignUpResponse>('/api/v1/creators', formData, {
        headers: { 'Accept-Language': 'pt-BR' },
      })
      .pipe(catchError(() => of(null)));
  }

  getMe(): Observable<MeResponse | null> {
    return this.http
      .get<MeResponse>('/api/v1/creators/me')
      .pipe(catchError(() => of(null)));
  }

  auth(request: AuthRequest): Observable<AuthResponse | null> {
    return this.http
      .post<AuthResponse>('/api/v1/auth', request)
      .pipe(catchError(() => of(null)));
  }

  refreshToken(): Observable<AuthResponse | null> {
    return this.http
      .post<AuthResponse>('/api/v1/auth/refresh', {}, { withCredentials: true })
      .pipe(catchError(() => of(null)));
  }
}
