import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, firstValueFrom, throwError } from 'rxjs';

interface Me {
  username: string;
}

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly http = inject(HttpClient);

  readonly user = signal<string | null>(null);

  /** True when a valid session cookie exists (checks the server once, then trusts the signal). */
  async ensureSession(): Promise<boolean> {
    if (this.user()) {
      return true;
    }
    try {
      const me = await firstValueFrom(this.http.get<Me>('/api/auth/me'));
      this.user.set(me.username);
      return true;
    } catch {
      return false;
    }
  }

  async login(username: string, password: string): Promise<void> {
    const me = await firstValueFrom(this.http.post<Me>('/api/auth/login', { username, password }));
    this.user.set(me.username);
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/api/auth/logout', {}));
    } finally {
      this.user.set(null);
    }
  }
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
