import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  ElementRef,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
// Official ALTCHA widget (web component <altcha-widget>) with the Polish texts; English is built in.
import 'altcha';
import 'altcha/i18n/pl';
import type { AltchaWidgetElement } from 'altcha/types/generic';
import { I18n } from '../../core/i18n/i18n';

/**
 * Bot check for the registration form: the widget fetches a signed challenge from the server and solves the
 * proof of work in Web Workers (starts as soon as the form gets focus, so it is usually done before "submit").
 */
@Component({
  selector: 'app-altcha',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  styles: `
    :host { display: block; }
    altcha-widget {
      --altcha-color-base: var(--card);
      --altcha-color-base-content: var(--foreground);
      --altcha-color-primary: var(--progress);
      --altcha-color-primary-content: var(--primary-foreground);
      --altcha-color-success: var(--success);
      --altcha-color-error: var(--destructive);
      --altcha-color-neutral: var(--border);
      --altcha-border-color: var(--border);
      --altcha-border-radius: 1rem;
      --altcha-max-width: 100%;
      --altcha-shadow: none;
      font-family: inherit;
    }
  `,
  template: `
    <altcha-widget
      #widget
      challenge="/api/auth/challenge"
      auto="onfocus"
      type="checkbox"
      [attr.language]="language()"
      (verified)="onVerified($event)"
      (expired)="payload.set(null)"
    ></altcha-widget>
  `,
})
export class AltchaField {
  private readonly i18n = inject(I18n);
  private readonly widget = viewChild.required<ElementRef<AltchaWidgetElement>>('widget');

  protected readonly language = computed(() => this.i18n.lang());
  readonly payload = signal<string | null>(null);

  constructor() {
    // hide the "Protected by ALTCHA" footer and logo: the form explains the check itself
    queueMicrotask(() => void this.widget().nativeElement.configure?.({ hideFooter: true, hideLogo: true }));
  }

  protected onVerified(event: Event): void {
    this.payload.set((event as CustomEvent<{ payload: string }>).detail.payload);
  }

  /** The solved payload; solves now if the widget has not finished yet. */
  async solve(): Promise<string> {
    const ready = this.payload();
    if (ready) {
      return ready;
    }
    const result = await this.widget().nativeElement.verify();
    if (!result?.payload) {
      throw new Error('ALTCHA not solved');
    }
    this.payload.set(result.payload);
    return result.payload;
  }

  /** A used or rejected solution can not be sent again: start over with a new challenge. */
  reset(): void {
    this.payload.set(null);
    this.widget().nativeElement.reset();
  }
}
