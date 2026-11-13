import { ChangeDetectionStrategy, Component, booleanAttribute, input } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideCheck } from '@ng-icons/lucide';

/** Round one-tap check (habits). Controlled: bind `checked` and handle `(click)`. */
@Component({
  selector: 'button[tlCheck]',
  imports: [NgIcon],
  viewProviders: [provideIcons({ lucideCheck })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    type: 'button',
    role: 'checkbox',
    '[attr.aria-checked]': 'checked()',
    class:
      'inline-flex size-9 shrink-0 items-center justify-center rounded-full border-2 text-lg transition active:scale-90',
    '[class.border-track]': '!checked()',
    '[class.bg-progress]': 'checked()',
    '[class.border-progress]': 'checked()',
    '[class.text-white]': 'checked()',
  },
  template: `
    @if (checked()) {
      <ng-icon name="lucideCheck" />
    }
  `,
})
export class TlCheck {
  readonly checked = input(false, { transform: booleanAttribute });
}
