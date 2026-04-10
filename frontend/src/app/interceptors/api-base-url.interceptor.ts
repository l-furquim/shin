import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../environments/environment';

function isAbsoluteUrl(url: string): boolean {
  return /^https?:\/\//i.test(url);
}

export const apiBaseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  if (!environment.apiBaseUrl || isAbsoluteUrl(req.url)) {
    return next(req);
  }

  const normalizedBaseUrl = environment.apiBaseUrl.replace(/\/+$/, '');
  const normalizedPath = req.url.startsWith('/') ? req.url : `/${req.url}`;

  return next(req.clone({ url: `${normalizedBaseUrl}${normalizedPath}`, withCredentials: true }));
};
