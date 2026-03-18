import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ZardIdDirective } from './shared/core';
import { ZardButtonComponent } from './shared/components/button';
import { ZardCardComponent } from './shared/components/card';
import { AuthService } from './features/auth/auth.service';
import { CreatorStore } from './core/stores/creator.store';
import { Creator } from './features/creator/creator.types';

type authState = 'login' | 'sign-up';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ZardCardComponent, ZardButtonComponent, ZardIdDirective],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('frontend');
  private authState = signal<authState>('login');
  private creatorState = signal<Creator | null>(null);

  loading = signal(false);
  feedback = signal('');

  private authService = inject(AuthService);
  private creatorStore = inject(CreatorStore);

  auth() {
    this.loading.set(true);

    try {
      const response = this.authService.auth({ email: '', password: '' });

      if (response == null) {
        this.feedback.set('Email or password is incorrect');

        return;
      }

      if (!this.creatorState) {
        // fetch user data again here

        return;
      }

      this.creatorStore.setCreator(this.creatorState);
    } finally {
      this.loading.set(false);
    }
  }

  onSignUp() {
    this.authState.set('sign-up');
  }
}
