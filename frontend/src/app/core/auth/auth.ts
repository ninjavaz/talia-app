import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, firstValueFrom, throwError } from 'rxjs';

interface Me {
  username: string;
  email: string | null;
}

export interface Registration {
  username: string;
  email: string;
  password: string;
  /** Base64 payload of the solved ALTCHA challenge (from the widget). */
  altcha: string;
  /** Honeypot - always empty for people. */
  website: string;
  language: string;
}

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly http = inject(HttpClient);

  readonly user = signal<string | null>(null);
  readonly email = signal<string | null>(null);

  /** True when a valid session cookie exists (checks the server once, then trusts the signal). */
  async ensureSession(): Promise<boolean> {
    if (this.user()) {
      return true;
    }
    try {
      this.setMe(await firstValueFrom(this.http.get<Me>('/api/auth/me')));
      return true;
    } catch {
      return false;
    }
  }

  async login(username: string, password: string): Promise<void> {
    this.setMe(await firstValueFrom(this.http.post<Me>('/api/auth/login', { username, password })));
  }

  /** Creates an unconfirmed account; the server e-mails a confirmation link (no session yet). */
  async register(registration: Registration): Promise<void> {
    await firstValueFrom(this.http.post('/api/auth/register', registration));
  }

  async verifyEmail(token: string): Promise<void> {
    await firstValueFrom(this.http.post('/api/auth/verify-email', { token }));
  }

  async forgotPassword(email: string): Promise<void> {
    await firstValueFrom(this.http.post('/api/auth/forgot-password', { email }));
  }

  async resetPassword(token: string, password: string): Promise<void> {
    await firstValueFrom(this.http.post('/api/auth/reset-password', { token, password }));
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/api/auth/logout', {}));
    } finally {
      this.user.set(null);
      this.email.set(null);
    }
  }

  private setMe(me: Me): void {
    this.user.set(me.username);
    this.email.set(me.email);
  }
}

/** Reads the one-time token from a link like /verify#t=...; the fragment never reaches any server log. */
export function readLinkToken(): string | null {
  const token = new URLSearchParams(location.hash.slice(1)).get('t');
  // drop the token from the address bar and history
  history.replaceState(history.state, '', location.pathname);
  return token;
}

export const authGuard: CanActivateFn = async () => {
  const auth = inject(Auth);
  const router = inject(Router);
  return (await auth.ensureSession()) || router.createUrlTree(['/login']);
};

/** A 401 on any API call (expired session) sends the user back to the login screen. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !req.url.startsWith('/api/auth/')
      ) {
        auth.user.set(null);
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
