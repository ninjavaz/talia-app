import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TlButton, TlCard, TlField, TlInput, TlLogo } from '@talia/ui';
import { Auth, readLinkToken } from '../../core/auth/auth';
import { TranslatePipe } from '../../core/i18n/i18n';
import { TranslationKey } from '../../core/i18n/translations';

/** Opened from the password reset e-mail: /reset#t=<token>. */
@Component({
  selector: 'app-reset-password',
  imports: [FormsModule, RouterLink, TlButton, TlCard, TlField, TlInput, TlLogo, TranslatePipe],
  template: `
    <div class="mx-auto flex min-h-dvh max-w-sm flex-col justify-center px-4 py-10">
      <div class="mb-8 flex flex-col items-center text-center">
        <tl-logo [size]="64" />
        <h1 class="mt-4 text-4xl font-semibold tracking-tight">{{ 'app.name' | t }}</h1>
      </div>
      <tl-card>
        @if (done()) {
          <div class="flex flex-col gap-4 text-center" role="status">
            <p class="font-medium">{{ 'reset.ok' | t }}</p>
            <a tlButton routerLink="/login" class="w-full">{{ 'verify.toLogin' | t }}</a>
          </div>
        } @else if (!token) {
          <div class="flex flex-col gap-4 text-center" role="alert">
            <p>{{ 'reset.invalid' | t }}</p>
            <a tlButton routerLink="/login" class="w-full">{{ 'login.back' | t }}</a>
          </div>
        } @else {
          <form class="flex flex-col gap-4" (ngSubmit)="save()">
            <h2 class="text-lg font-semibold">{{ 'reset.title' | t }}</h2>
            <tl-field [label]="'login.password' | t" [hint]="'register.passwordHint' | t">
              <input tlInput name="password" type="password" autocomplete="new-password" minlength="10" required
                     [(ngModel)]="password" />
            </tl-field>
            <tl-field [label]="'register.repeat' | t">
              <input tlInput name="repeat" type="password" autocomplete="new-password" required [(ngModel)]="repeat" />
            </tl-field>
            @if (error(); as key) {
              <p class="text-destructive text-sm" role="alert">{{ key | t }}</p>
            }
            <button tlButton type="submit" class="mt-2 w-full" [disabled]="busy()">{{ 'reset.submit' | t }}</button>
          </form>
        }
      </tl-card>
    </div>
  `,
})
export class ResetPasswordPage {
  private readonly auth = inject(Auth);

  protected readonly token = readLinkToken();
  protected readonly password = signal('');
  protected readonly repeat = signal('');
  protected readonly busy = signal(false);
  protected readonly done = signal(false);
  protected readonly error = signal<TranslationKey | null>(null);

  protected async save(): Promise<void> {
    if (this.busy() || !this.token) {
      return;
    }
    if (this.password().length < 10) {
      this.error.set('register.tooShort');
      return;
    }
    if (this.password() !== this.repeat()) {
      this.error.set('register.mismatch');
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await this.auth.resetPassword(this.token, this.password());
      this.password.set('');
      this.repeat.set('');
      this.done.set(true);
    } catch (e) {
      // 400 is either a weak password or a dead link; the link check comes first on the server
      const status = e instanceof HttpErrorResponse ? e.status : 0;
      const detail = e instanceof HttpErrorResponse ? String(e.error?.detail ?? '') : '';
      this.error.set(
        status === 400 ? (detail.includes('link') ? 'reset.invalid' : 'reset.weak') : 'login.networkError',
      );
    } finally {
      this.busy.set(false);
    }
  }
}
