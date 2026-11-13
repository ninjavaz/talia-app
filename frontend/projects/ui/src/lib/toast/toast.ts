import { ChangeDetectionStrategy, Component, Injectable, inject, signal } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideCircleAlert, lucideCircleCheck } from '@ng-icons/lucide';

export type TlToastTone = 'success' | 'error';

interface ToastState {
  id: number;
  message: string;
  tone: TlToastTone;
}

/** Short confirmation pill at the top of the screen. Put one `<tl-toaster />` in the app root. */
@Injectable({ providedIn: 'root' })
export class TlToast {
  readonly current = signal<ToastState | null>(null);
  private nextId = 0;

  success(message: string): void {
    this.show(message, 'success');
  }

  error(message: string): void {
    this.show(message, 'error', 4000);
  }

  show(message: string, tone: TlToastTone, durationMs = 2500): void {
    const id = ++this.nextId;
    this.current.set({ id, message, tone });
    setTimeout(() => {
      if (this.current()?.id === id) {
        this.current.set(null);
      }
    }, durationMs);
  }
}

@Component({
  selector: 'tl-toaster',
  imports: [NgIcon],
  viewProviders: [provideIcons({ lucideCircleCheck, lucideCircleAlert })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .toast { animation: drop 260ms cubic-bezier(0.2, 0.9, 0.3, 1.2); }
    @keyframes drop { from { transform: translateY(-140%); opacity: 0; } }
  `,
  template: `
    <div
      class="pointer-events-none fixed inset-x-0 top-[calc(env(safe-area-inset-top)+0.75rem)] z-70 flex justify-center px-4"
      role="status"
      aria-live="polite"
    >
      @if (toast.current(); as t) {
        @let success = t.tone === 'success';
        <div
          class="toast pointer-events-auto flex cursor-pointer items-center gap-2 rounded-full px-5 py-3 text-sm font-semibold shadow-lg shadow-black/15"
          [class]="success ? 'bg-success text-white dark:text-background' : 'bg-destructive text-white'"
          (click)="toast.current.set(null)"
        >
          <ng-icon [name]="success ? 'lucideCircleCheck' : 'lucideCircleAlert'" class="text-lg" />
          {{ t.message }}
        </div>
      }
    </div>
  `,
})
export class TlToaster {
  protected readonly toast = inject(TlToast);
}
