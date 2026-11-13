import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TlButton, TlCard, TlField, TlInput, TlLogo, TlSegmented } from '@talia/ui';
import { Auth } from '../../core/auth/auth';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';
import { TranslationKey } from '../../core/i18n/translations';

type Mode = 'login' | 'register';

const MIN_PASSWORD = 10;

@Component({
  selector: 'app-login',
  imports: [FormsModule, TlButton, TlCard, TlField, TlInput, TlLogo, TlSegmented, TranslatePipe],
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

      <tl-segmented class="mb-4" [options]="modes()" [value]="mode()" (valueChange)="switchMode($event)" />

      <tl-card>
        @if (mode() === 'login') {
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
          </form>
        } @else {
          <form class="relative flex flex-col gap-4" (ngSubmit)="register()">
            <tl-field [label]="'register.username' | t" [hint]="'register.usernameHint' | t">
              <input tlInput name="username" autocomplete="username" autocapitalize="none" autocorrect="off"
                     spellcheck="false" maxlength="30" required [(ngModel)]="username" />
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
            @if (busy()) {
              <p class="text-muted-foreground text-sm" role="status">{{ 'register.checking' | t }}</p>
            }
            @if (error(); as key) {
              <p class="text-destructive text-sm" role="alert">{{ key | t }}</p>
            }
            <button tlButton type="submit" class="mt-2 w-full" [disabled]="busy()">
              {{ 'register.submit' | t }}
            </button>
          </form>
        }
      </tl-card>
    </div>
  `,
})
export class LoginPage {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18n);

  protected readonly mode = signal<Mode>('login');
  protected readonly username = signal('');
  protected readonly password = signal('');
  protected readonly repeat = signal('');
  protected readonly website = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<TranslationKey | null>(null);

  protected readonly modes = computed(() => [
    { value: 'login', label: this.i18n.t('login.tabLogin') },
    { value: 'register', label: this.i18n.t('login.tabRegister') },
  ]);

  protected switchMode(value: string | undefined): void {
    if (value === 'login' || value === 'register') {
      this.mode.set(value);
      this.error.set(null);
      this.password.set('');
      this.repeat.set('');
    }
  }

  protected async login(): Promise<void> {
    if (!this.username().trim() || !this.password() || this.busy()) {
      return;
    }
    await this.run(() => this.auth.login(this.username().trim(), this.password()), {
      401: 'login.error',
    });
  }

  protected async register(): Promise<void> {
    if (this.busy()) {
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
    await this.run(() => this.auth.register(this.username().trim(), this.password(), this.website()), {
      400: 'register.invalid',
      409: 'register.taken',
    });
  }

  private async run(action: () => Promise<void>, errors: Record<number, TranslationKey>): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
      this.password.set('');
      this.repeat.set('');
      await this.router.navigateByUrl('/');
    } catch (e) {
      const status = e instanceof HttpErrorResponse ? e.status : 0;
      this.error.set(status === 429 ? 'common.tooMany' : (errors[status] ?? 'login.networkError'));
    } finally {
      this.busy.set(false);
    }
  }
}
