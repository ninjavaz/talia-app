import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TlCard } from '@talia/ui';
import { HlmButtonImports } from '@ui/button';
import { HlmInputImports } from '@ui/input';
import { HlmLabelImports } from '@ui/label';
import { Auth } from '../../core/auth/auth';
import { TranslatePipe } from '../../core/i18n/i18n';
import { TranslationKey } from '../../core/i18n/translations';

@Component({
  selector: 'app-login',
  imports: [FormsModule, HlmButtonImports, HlmInputImports, HlmLabelImports, TlCard, TranslatePipe],
  template: `
    <div class="mx-auto flex min-h-dvh max-w-sm flex-col justify-center px-4">
      <h1 class="text-center text-5xl font-semibold tracking-tight">{{ 'app.name' | t }}</h1>
      <p class="text-muted-foreground mt-2 mb-8 text-center text-sm">{{ 'login.subtitle' | t }}</p>

      <tl-card>
        <form class="flex flex-col gap-4" (ngSubmit)="submit()">
          <div class="flex flex-col gap-2">
            <label hlmLabel for="username">{{ 'login.username' | t }}</label>
            <input
              hlmInput
              id="username"
              name="username"
              autocomplete="username"
              autocapitalize="none"
              required
              class="h-12 rounded-2xl px-4 text-base"
              [(ngModel)]="username"
            />
          </div>
          <div class="flex flex-col gap-2">
            <label hlmLabel for="password">{{ 'login.password' | t }}</label>
            <input
              hlmInput
              id="password"
              name="password"
              type="password"
              autocomplete="current-password"
              required
              class="h-12 rounded-2xl px-4 text-base"
              [(ngModel)]="password"
            />
          </div>
          @if (error(); as key) {
            <p class="text-destructive text-sm" role="alert">{{ key | t }}</p>
          }
          <button hlmBtn type="submit" class="mt-2 h-12 w-full rounded-full text-base" [disabled]="busy()">
            {{ 'login.submit' | t }}
          </button>
        </form>
      </tl-card>
    </div>
  `,
})
export class LoginPage {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  protected username = '';
  protected password = '';
  protected readonly busy = signal(false);
  protected readonly error = signal<TranslationKey | null>(null);

  protected async submit(): Promise<void> {
    if (!this.username || !this.password || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await this.auth.login(this.username.trim(), this.password);
      await this.router.navigateByUrl('/');
    } catch (e) {
      const unauthorized = e instanceof HttpErrorResponse && e.status === 401;
      this.error.set(unauthorized ? 'login.error' : 'login.networkError');
    } finally {
      this.busy.set(false);
    }
  }
}
