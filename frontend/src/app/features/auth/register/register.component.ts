import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '@/features/auth/auth.service';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardInputDirective } from '@/shared/components/input';

@Component({
  selector: 'app-register-page',
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
    <main class="mx-auto flex min-h-screen w-full max-w-7xl items-center justify-center px-4 py-10 md:px-8">
      <z-card
        class="w-full max-w-md border-stone-200/70 bg-white/80 backdrop-blur-sm"
        zTitle="Criar conta"
        zDescription="Cadastre-se para acessar o painel de criador"
      >
        <form class="space-y-4" [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="space-y-2">
            <label for="displayName" class="text-sm font-medium">Nome</label>
            <input id="displayName" z-input type="text" formControlName="displayName" placeholder="Seu nome" />
          </div>

          <div class="space-y-2">
            <label for="email" class="text-sm font-medium">Email</label>
            <input id="email" z-input type="email" formControlName="email" placeholder="m@example.com" />
          </div>

          <div class="space-y-2">
            <label for="password" class="text-sm font-medium">Senha</label>
            <input id="password" z-input type="password" formControlName="password" placeholder="********" />
          </div>

          <div class="space-y-2">
            <label for="username" class="text-sm font-medium">Username (opcional)</label>
            <input id="username" z-input type="text" formControlName="username" placeholder="@seucanal" />
          </div>

          <div class="space-y-2">
            <label for="description" class="text-sm font-medium">Descricao (opcional)</label>
            <textarea
              id="description"
              z-input
              formControlName="description"
              placeholder="Fale um pouco sobre seu canal"
            ></textarea>
          </div>

          <label class="flex items-center gap-2 text-sm">
            <input type="checkbox" formControlName="showAdultContent" />
            Permitir conteudo adulto
          </label>

          @if (errorMessage()) {
            <z-alert zType="destructive" zTitle="Falha no cadastro" [zDescription]="errorMessage()"></z-alert>
          }

          @if (successMessage()) {
            <z-alert zTitle="Cadastro concluido" [zDescription]="successMessage()" zIcon="circle-check"></z-alert>
          }

          <div card-footer class="w-full items-start gap-3 px-0">
            <z-button zType="default" [zDisabled]="isSubmitting() || form.invalid" [zLoading]="isSubmitting()">
              {{ isSubmitting() ? 'Criando conta...' : 'Cadastrar' }}
            </z-button>
            <p class="text-muted-foreground text-sm">
              Ja possui conta?
              <a routerLink="/login" class="text-foreground font-medium underline underline-offset-4">Entrar</a>
            </p>
          </div>
        </form>
      </z-card>
    </main>
  `,
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');

  protected readonly form = this.fb.nonNullable.group({
    displayName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    username: ['', [Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(500)]],
    showAdultContent: [false],
  });

  async onSubmit(): Promise<void> {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    try {
      const response = await firstValueFrom(
        this.authService.createCreator({
          displayName: this.form.controls.displayName.value,
          email: this.form.controls.email.value,
          password: this.form.controls.password.value,
          showAdultContent: this.form.controls.showAdultContent.value,
          username: this.form.controls.username.value,
          description: this.form.controls.description.value,
        }),
      );

      if (!response) {
        this.errorMessage.set('Nao foi possivel cadastrar. Verifique os dados e tente novamente.');
        return;
      }

      this.successMessage.set('Conta criada com sucesso. Redirecionando para login...');

      setTimeout(() => {
        void this.router.navigateByUrl('/login');
      }, 1000);
    } catch {
      this.errorMessage.set('Erro inesperado durante cadastro.');
    } finally {
      this.isSubmitting.set(false);
    }
  }
}
