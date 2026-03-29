import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';
import type {
  AuthRequest,
  AuthResponse,
  CreatorSignUpRequest,
  CreatorSignUpResponse,
  MeResponse,
  RegisterRequest,
  RegisterResponse,
  UserAuthProfile,
} from './auth.types';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private deviceId: string | null = null;

  register(request: RegisterRequest): Observable<RegisterResponse | null> {
    return this.http.post<RegisterResponse>('/auth/register', request).pipe(
      catchError((err) => {
        console.log('erro', err);
        return of(null);
      }),
    );
  }

  createCreator(request: CreatorSignUpRequest): Observable<CreatorSignUpResponse | null> {
    const formData = new FormData();
    formData.append(
      'data',
      new Blob([JSON.stringify(request)], {
        type: 'application/json',
      }),
    );

    return this.http
      .post<CreatorSignUpResponse>('/api/v1/creators', formData, {
        headers: {
          'Accept-Language': 'pt-BR',
        },
      })
      .pipe(
        catchError((err) => {
          console.log('erro create creator', err);
          return of(null);
        }),
      );
  }

  getUserByEmail(email: string): Observable<UserAuthProfile | null> {
    return this.http
      .post<UserAuthProfile>('/api/v1/users/auth', {
        email,
      })
      .pipe(
        catchError((err) => {
          console.log('erro user auth', err);
          return of(null);
        }),
      );
  }

  getMe(userId: string): Observable<MeResponse | null> {
    return this.http
      .get<MeResponse>('/api/v1/creators/me', {
        headers: {
          'X-User-Id': userId,
        },
      })
      .pipe(
        catchError((err) => {
          console.log('erro get me', err);
          return of(null);
        }),
      );
  }

  auth(request: AuthRequest): Observable<AuthResponse | null> {
    return this.http.post<AuthResponse>('/api/v1/auth', request).pipe(
      map((response) => {
        this.deviceId = response.deviceId;
        return response;
      }),

      catchError((err) => {
        console.log('erro', err);
        return of(null);
      }),
    );
  }

  refreshToken(): Observable<AuthResponse | null> {
    return this.http
      .post<AuthResponse>(
        '/auth/refresh',
        {},
        {
          withCredentials: true,
        },
      )
      .pipe(
        catchError((err) => {
          console.log('erro refresh', err);
          return of(null);
        }),
      );
  }

  getDeviceId(): string | null {
    return this.deviceId;
  }
}
