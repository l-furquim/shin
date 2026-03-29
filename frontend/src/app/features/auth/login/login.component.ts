import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { CreatorStore } from '@/core/stores/creator.store';
import { AuthService } from '@/features/auth/auth.service';
import { TokenService } from '@/features/auth/token.service';
import type { Creator } from '@/features/creator/creator.types';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardInputDirective } from '@/shared/components/input';

@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ZardCardComponent,
    ZardInputDirective,
    ZardButtonComponent,
    ZardAlertComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main
      class="mx-auto flex min-h-screen w-full max-w-7xl items-center justify-center px-4 py-10 md:px-8"
    >
      <z-card
        class="w-full max-w-md border-stone-200/70 bg-white/80 backdrop-blur-sm"
        zTitle="Entrar"
        zDescription="Acesse sua conta para enviar e gerenciar videos"
      >
        <form class="space-y-4" [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="space-y-2">
            <label for="email" class="text-sm font-medium">Email</label>
            <input
              id="email"
              z-input
              type="email"
              formControlName="email"
              placeholder="m@example.com"
            />
          </div>

          <div class="space-y-2">
            <label for="password" class="text-sm font-medium">Senha</label>
            <input
              id="password"
              z-input
              type="password"
              formControlName="password"
              placeholder="********"
            />
          </div>

          @if (errorMessage()) {
            <z-alert zType="destructive" [zDescription]="errorMessage()"></z-alert>
          }

          <div card-footer class="w-full items-start gap-3 px-0">
            <button [disabled]="isSubmitting() || form.invalid" type="submit">
              {{ isSubmitting() ? 'Entrando...' : 'Entrar' }}
            </button>
            <p class="text-muted-foreground text-sm">
              Nao possui conta?
              <a
                routerLink="/register"
                class="text-foreground font-medium underline underline-offset-4"
                >Criar conta</a
              >
            </p>
          </div>
        </form>
      </z-card>
    </main>
  `,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly tokenService = inject(TokenService);
  private readonly creatorStore = inject(CreatorStore);
  private readonly router = inject(Router);

  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  async onSubmit(): Promise<void> {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();

      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    try {
      const deviceId = this.authService.getDeviceId();
      const response = await firstValueFrom(
        this.authService.auth({
          email: this.form.controls.email.value,
          password: this.form.controls.password.value,
          deviceId,
        }),
      );

      if (!response) {
        this.errorMessage.set('Credenciais invalidas.');
        return;
      }

      this.tokenService.setAccessToken(response.token);

      const user = await firstValueFrom(
        this.authService.getUserByEmail(this.form.controls.email.value),
      );

      if (!user) {
        this.errorMessage.set('Nao foi possivel carregar perfil do usuario.');
        return;
      }

      const me = await firstValueFrom(this.authService.getMe(user.id));

      if (!me) {
        this.errorMessage.set('Nao foi possivel carregar perfil de criador.');
        return;
      }

      const creator: Creator = {
        id: me.id,
        displayName: me.displayName,
        email: me.email,
        showAdultContent: me.showAdultContent,
        locale: me.locale,
        avatar: me.avatar,
        banner: me.banner,
        deviceId: response.deviceId,
        updatedAt: new Date(me.lastUpdate),
        createdAt: new Date(me.createdAt),
      };

      this.creatorStore.setCreator(creator);

      await this.router.navigateByUrl('/');
    } catch {
      this.errorMessage.set('Nao foi possivel autenticar. Tente novamente.');
    } finally {
      this.isSubmitting.set(false);
    }
  }
}
