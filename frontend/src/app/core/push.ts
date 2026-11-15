import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { SwPush } from '@angular/service-worker';
import { firstValueFrom } from 'rxjs';

/**
 * Web Push on this device (REQ-033) via the Angular service worker. Push only works where the service worker runs:
 * production builds, and on iPhone only in the app added to the home screen (iOS 16.4+).
 */
@Injectable({ providedIn: 'root' })
export class PushNotifications {
  private readonly swPush = inject(SwPush);
  private readonly http = inject(HttpClient);

  readonly supported =
    this.swPush.isEnabled &&
    typeof Notification !== 'undefined' &&
    typeof PushManager !== 'undefined';
  readonly permission = signal<NotificationPermission>(
    typeof Notification !== 'undefined' ? Notification.permission : 'denied',
  );
  /** Whether this device currently has a push subscription. */
  readonly subscribed = signal(false);

  constructor() {
    if (this.supported) {
      this.swPush.subscription.subscribe((sub) => this.subscribed.set(sub !== null));
    }
  }

  /** Asks for permission (must be called from a tap on iOS) and registers this device on the server. */
  async enable(): Promise<void> {
    const { publicKey } = await firstValueFrom(
      this.http.get<{ publicKey: string }>('/api/push/key'),
    );
    try {
      const sub = await this.swPush.requestSubscription({ serverPublicKey: publicKey });
      await this.register(sub);
    } finally {
      this.permission.set(Notification.permission);
    }
  }

  async disable(): Promise<void> {
    const sub = await firstValueFrom(this.swPush.subscription);
    if (sub) {
      await firstValueFrom(
        this.http.delete('/api/push/subscriptions', { body: { endpoint: sub.endpoint } }),
      );
      await this.swPush.unsubscribe();
    }
  }

  /** Sends a test notification to all devices of the user; resolves with how many accepted it. */
  async test(): Promise<number> {
    return (await firstValueFrom(this.http.post<{ sent: number }>('/api/push/test', null))).sent;
  }

  /**
   * On app start: re-registers an existing subscription (it may belong to another account now, or the server lost
   * it), and subscribes again when the server's key changed (e.g. a development server without fixed keys).
   */
  async sync(): Promise<void> {
    if (!this.supported || Notification.permission !== 'granted') {
      return;
    }
    try {
      const sub = await firstValueFrom(this.swPush.subscription);
      if (!sub) {
        return;
      }
      const { publicKey } = await firstValueFrom(
        this.http.get<{ publicKey: string }>('/api/push/key'),
      );
      if (keyOf(sub) === publicKey) {
        await this.register(sub);
      } else {
        await sub.unsubscribe();
        await this.register(await this.swPush.requestSubscription({ serverPublicKey: publicKey }));
      }
    } catch {
      // not critical: the user can enable notifications again in Settings
    }
  }

  private async register(sub: PushSubscription): Promise<void> {
    await firstValueFrom(this.http.put('/api/push/subscriptions', sub.toJSON()));
    this.subscribed.set(true);
  }
}

/** The subscription's applicationServerKey as base64url (the server's format). */
function keyOf(sub: PushSubscription): string | null {
  const key = sub.options.applicationServerKey;
  if (!key) {
    return null;
  }
  const bytes = new Uint8Array(key);
  let binary = '';
  bytes.forEach((b) => (binary += String.fromCharCode(b)));
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
