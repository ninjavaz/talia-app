import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TlButton, TlCard, TlLogo } from '@talia/ui';
import { Auth, readLinkToken } from '../../core/auth/auth';
import { TranslatePipe } from '../../core/i18n/i18n';

/** Opened from the confirmation e-mail: /verify#t=<token>. */
@Component({
  selector: 'app-verify-email',
  imports: [RouterLink, TlButton, TlCard, TlLogo, TranslatePipe],
  template: `
    <div class="mx-auto flex min-h-dvh max-w-sm flex-col justify-center px-4 py-10">
      <div class="mb-8 flex flex-col items-center text-center">
        <tl-logo [size]="64" />
        <h1 class="mt-4 text-4xl font-semibold tracking-tight">{{ 'app.name' | t }}</h1>
      </div>
      <tl-card>
        <div class="flex flex-col gap-4 text-center" role="status">
          @switch (state()) {
            @case ('checking') {
              <p>{{ 'verify.checking' | t }}</p>
            }
            @case ('ok') {
              <p class="font-medium">{{ 'verify.ok' | t }}</p>
            }
            @default {
              <p>{{ 'verify.invalid' | t }}</p>
            }
          }
          @if (state() !== 'checking') {
            <a tlButton routerLink="/login" class="w-full">{{ 'verify.toLogin' | t }}</a>
          }
        </div>
      </tl-card>
    </div>
  `,
})
export class VerifyEmailPage {
  protected readonly state = signal<'checking' | 'ok' | 'invalid'>('checking');

  constructor() {
    const auth = inject(Auth);
    const token = readLinkToken();
    if (!token) {
      this.state.set('invalid');
      return;
    }
    auth.verifyEmail(token).then(
      () => this.state.set('ok'),
      () => this.state.set('invalid'),
    );
  }
}
