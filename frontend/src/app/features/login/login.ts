import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TlButton, TlCard, TlField, TlInput, TlLogo, TlSegmented } from '@talia/ui';
import { Auth } from '../../core/auth/auth';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';
import { TranslationKey } from '../../core/i18n/translations';
import { AltchaField } from './altcha';

/** login / register = the two tabs; forgot = "forgot password" form; sent = "check your inbox" after one of them. */
type View = 'login' | 'register' | 'forgot' | 'registered' | 'forgotSent';

const MIN_PASSWORD = 10;
const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/;

@Component({
  selector: 'app-login',
  imports: [FormsModule, TlButton, TlCard, TlField, TlInput, TlLogo, TlSegmented, TranslatePipe, AltchaField],
  styles: `
    /* Honeypot: present in the DOM for bots, invisible and unreachable for people and screen readers. */
    .hp { position: absolute; left: -10000px; width: 1px; height: 1px; overflow: hidden; }
  `,
  template: `
    <div class="mx-auto flex min-h-dvh max-w-sm flex-col justify-center px-4 py-10">
      <div class="mb-8 flex flex-col items-center text-center">
        <tl-logo [size]="76" />
        <h1 class="mt-5 text-5xl font-semibold tracking-tight">{{ 'app.name' | t }}</h1>
        <p class="mt-3 text-lg font-medium">{{ 'login.slogan' | t }}</p>
        <p class="text-muted-foreground mt-1 text-sm">{{ 'login.subtitle' | t }}</p>
      </div>

      @if (view() === 'login' || view() === 'register') {
        <tl-segmented class="mb-4" [options]="tabs()" [value]="view()" (valueChange)="switchTab($event)" />
      }

      <tl-card>
        @switch (view()) {
          @case ('login') {
            <form class="flex flex-col gap-4" (ngSubmit)="login()">
              <tl-field [label]="'login.username' | t">
                <input tlInput name="username" autocomplete="username" autocapitalize="none" autocorrect="off"
                       spellcheck="false" required [(ngModel)]="username" />
              </tl-field>
              <tl-field [label]="'login.password' | t">
                <input tlInput name="password" type="password" autocomplete="current-password" required
                       [(ngModel)]="password" />
              </tl-field>
              @if (error(); as key) {
                <p class="text-destructive text-sm" role="alert">{{ key | t }}</p>
              }
              <button tlButton type="submit" class="mt-2 w-full" [disabled]="busy()">{{ 'login.submit' | t }}</button>
              <button type="button" class="text-muted-foreground self-center text-sm underline-offset-4 hover:underline"
                      (click)="go('forgot')">
                {{ 'login.forgot' | t }}
              </button>
            </form>
          }
          @case ('register') {
            <form class="relative flex flex-col gap-4" (ngSubmit)="register()">
              <tl-field [label]="'register.username' | t" [hint]="'register.usernameHint' | t">
                <input tlInput name="username" autocomplete="username" autocapitalize="none" autocorrect="off"
                       spellcheck="false" maxlength="30" required [(ngModel)]="username" />
              </tl-field>
              <tl-field [label]="'register.email' | t" [hint]="'register.emailHint' | t">
                <input tlInput name="email" type="email" autocomplete="email" autocapitalize="none"
                       spellcheck="false" maxlength="254" required [(ngModel)]="email" />
              </tl-field>
              <tl-field [label]="'login.password' | t" [hint]="'register.passwordHint' | t">
                <input tlInput name="password" type="password" autocomplete="new-password" minlength="10" required
                       [(ngModel)]="password" />
              </tl-field>
              <tl-field [label]="'register.repeat' | t">
                <input tlInput name="repeat" type="password" autocomplete="new-password" required [(ngModel)]="repeat" />
              </tl-field>
              <div class="hp" aria-hidden="true">
                <label>Website <input name="website" tabindex="-1" autocomplete="off" [(ngModel)]="website" /></label>
              </div>
              <app-altcha />
              @if (error(); as key) {
                <p class="text-destructive text-sm" role="alert">{{ key | t }}</p>
              }
              <button tlButton type="submit" class="mt-2 w-full" [disabled]="busy()">
                {{ 'register.submit' | t }}
              </button>
            </form>
          }
          @case ('forgot') {
            <form class="flex flex-col gap-4" (ngSubmit)="forgot()">
              <div>
                <h2 class="text-lg font-semibold">{{ 'forgot.title' | t }}</h2>
                <p class="text-muted-foreground mt-1 text-sm">{{ 'forgot.text' | t }}</p>
              </div>
              <tl-field [label]="'register.email' | t">
                <input tlInput name="email" type="email" autocomplete="email" autocapitalize="none"
                       spellcheck="false" maxlength="254" required [(ngModel)]="email" />
              </tl-field>
              @if (error(); as key) {
                <p class="text-destructive text-sm" role="alert">{{ key | t }}</p>
              }
              <button tlButton type="submit" class="w-full" [disabled]="busy()">{{ 'forgot.submit' | t }}</button>
              <button type="button" class="text-muted-foreground self-center text-sm underline-offset-4 hover:underline"
                      (click)="go('login')">
                {{ 'login.back' | t }}
              </button>
            </form>
          }
          @default {
            <div class="flex flex-col gap-3 text-center" role="status">
              <h2 class="text-lg font-semibold">{{ 'register.sentTitle' | t }}</h2>
              @if (view() === 'registered') {
                <p class="text-sm">{{ 'register.sentText' | t: { email: email().trim() } }}</p>
                <p class="text-muted-foreground text-sm">{{ 'register.sentHint' | t }}</p>
              } @else {
                <p class="text-sm">{{ 'forgot.sent' | t: { email: email().trim() } }}</p>
              }
              <button tlButton type="button" class="mt-2 w-full" (click)="go('login')">{{ 'login.back' | t }}</button>
            </div>
          }
        }
      </tl-card>
    </div>
  `,
})
export class LoginPage {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18n);
  private readonly altcha = viewChild(AltchaField);

  protected readonly view = signal<View>('login');
  protected readonly username = signal('');
  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly repeat = signal('');
  protected readonly website = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<TranslationKey | null>(null);

  protected readonly tabs = computed(() => [
    { value: 'login', label: this.i18n.t('login.tabLogin') },
    { value: 'register', label: this.i18n.t('login.tabRegister') },
  ]);

  protected switchTab(value: string | undefined): void {
    if (value === 'login' || value === 'register') {
      this.go(value);
    }
  }

  protected go(view: View): void {
    this.view.set(view);
    this.error.set(null);
    this.password.set('');
    this.repeat.set('');
  }

  protected async login(): Promise<void> {
    if (!this.username().trim() || !this.password() || this.busy()) {
      return;
    }
    await this.run(
      async () => {
        await this.auth.login(this.username().trim(), this.password());
        this.password.set('');
        await this.router.navigateByUrl('/');
      },
      { 401: 'login.error', 403: 'login.notVerified' },
    );
  }

  protected async register(): Promise<void> {
    if (this.busy()) {
      return;
    }
    if (!EMAIL.test(this.email().trim())) {
      this.error.set('register.emailInvalid');
      return;
    }
    if (this.password().length < MIN_PASSWORD) {
      this.error.set('register.tooShort');
      return;
    }
    if (this.password() !== this.repeat()) {
      this.error.set('register.mismatch');
      return;
    }
    const altcha = this.altcha();
    await this.run(
      async () => {
        let payload: string;
        try {
          payload = await altcha!.solve();
        } catch {
          altcha?.reset();
          this.error.set('register.botFailed');
          return;
        }
        try {
          await this.auth.register({
            username: this.username().trim(),
            email: this.email().trim(),
            password: this.password(),
            altcha: payload,
            website: this.website(),
            language: this.i18n.lang(),
          });
        } finally {
          // every solution is accepted once: the next attempt needs a new one
          altcha?.reset();
        }
        this.go('registered');
      },
      { 400: 'register.invalid', 409: 'register.taken' },
    );
  }

  protected async forgot(): Promise<void> {
    if (this.busy()) {
      return;
    }
    if (!EMAIL.test(this.email().trim())) {
      this.error.set('register.emailInvalid');
      return;
    }
    await this.run(
      async () => {
        await this.auth.forgotPassword(this.email().trim());
        this.go('forgotSent');
      },
      { 400: 'register.emailInvalid' },
    );
  }

  private async run(action: () => Promise<void>, errors: Record<number, TranslationKey>): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
    } catch (e) {
      const status = e instanceof HttpErrorResponse ? e.status : 0;
      this.error.set(status === 429 ? 'common.tooMany' : (errors[status] ?? 'login.networkError'));
    } finally {
      this.busy.set(false);
    }
  }
}
