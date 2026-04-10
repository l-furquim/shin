import type { ApplicationConfig } from '@angular/core';
import { provideBrowserGlobalErrorListeners, provideAppInitializer, inject } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideZard } from '@/shared/core/provider/providezard';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { authInterceptor } from '@/interceptors/auth.interceptor';
import { apiBaseUrlInterceptor } from '@/interceptors/api-base-url.interceptor';
import { SessionBootstrapService } from '@/core/session-bootstrap.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptors([apiBaseUrlInterceptor, authInterceptor]), withFetch()),
    provideAppInitializer(() => inject(SessionBootstrapService).initialize()),
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideClientHydration(withEventReplay()),
    provideZard(),
  ],
};
