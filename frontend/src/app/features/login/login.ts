import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HlmButtonImports } from '@ui/button';
import { HlmCardImports } from '@ui/card';
import { HlmInputImports } from '@ui/input';
import { HlmLabelImports } from '@ui/label';
import { Auth } from '../../core/auth/auth';
import { TranslatePipe } from '../../core/i18n/i18n';
import { TranslationKey } from '../../core/i18n/translations';

@Component({
  selector: 'app-login',
  imports: [FormsModule, HlmButtonImports, HlmCardImports, HlmInputImports, HlmLabelImports, TranslatePipe],
  template: `
    <div class="flex min-h-dvh items-center justify-center px-4">
      <section hlmCard class="w-full max-w-sm">
        <div hlmCardHeader>
          <h1 hlmCardTitle class="text-primary text-3xl font-bold">{{ 'app.name' | t }}</h1>
          <p hlmCardDescription>{{ 'login.subtitle' | t }}</p>
        </div>
        <form hlmCardContent class="flex flex-col gap-4" (ngSubmit)="submit()">
          <div class="flex flex-col gap-2">
            <label hlmLabel for="username">{{ 'login.username' | t }}</label>
            <input
              hlmInput
              id="username"
              name="username"
              autocomplete="username"
              autocapitalize="none"
              required
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
              [(ngModel)]="password"
            />
          </div>
          @if (error(); as key) {
            <p class="text-destructive text-sm" role="alert">{{ key | t }}</p>
          }
          <button hlmBtn size="lg" type="submit" class="w-full" [disabled]="busy()">
            {{ 'login.submit' | t }}
          </button>
        </form>
      </section>
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
