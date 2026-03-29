import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';

@Injectable()
export class ApiBaseUrlInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!environment.apiBaseUrl || this.isAbsoluteUrl(req.url)) {
      return next.handle(req);
    }

    const normalizedBaseUrl = environment.apiBaseUrl.replace(/\/+$/, '');
    const normalizedPath = req.url.startsWith('/') ? req.url : `/${req.url}`;

    return next.handle(
      req.clone({
        url: `${normalizedBaseUrl}${normalizedPath}`,
      }),
    );
  }

  private isAbsoluteUrl(url: string): boolean {
    return /^https?:\/\//i.test(url);
  }
}
