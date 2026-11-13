import { A11yModule } from '@angular/cdk/a11y';
import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideX } from '@ng-icons/lucide';

/**
 * Bottom sheet for forms (iOS style). Controlled with `[(open)]`; closes on backdrop tap, Escape or the X button.
 *
 * <tl-sheet [(open)]="editing" title="Pomiar">...form...</tl-sheet>
 */
@Component({
  selector: 'tl-sheet',
  imports: [A11yModule, NgIcon],
  viewProviders: [provideIcons({ lucideX })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'close()' },
  styles: `
    .backdrop { animation: fade 160ms ease-out; }
    .panel { animation: rise 220ms cubic-bezier(0.2, 0.9, 0.3, 1); }
    @keyframes fade { from { opacity: 0; } }
    @keyframes rise { from { transform: translateY(100%); } }
  `,
  template: `
    @if (open()) {
      <div class="backdrop fixed inset-0 z-50 bg-black/40" (click)="close()"></div>
      <section
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="title()"
        cdkTrapFocus
        [cdkTrapFocusAutoCapture]="true"
        class="panel bg-card text-card-foreground fixed inset-x-0 bottom-0 z-50 mx-auto max-h-[90dvh] max-w-md overflow-y-auto rounded-t-[1.75rem] px-5 pt-3 pb-[calc(env(safe-area-inset-bottom)+1.25rem)] shadow-2xl"
      >
        <div class="bg-track mx-auto mb-3 h-1.5 w-10 rounded-full"></div>
        <header class="mb-4 flex items-center justify-between gap-3">
          <h2 class="text-lg font-semibold">{{ title() }}</h2>
          <button
            type="button"
            class="bg-chip text-chip-foreground inline-flex size-8 items-center justify-center rounded-full"
            [attr.aria-label]="closeLabel()"
            (click)="close()"
          >
            <ng-icon name="lucideX" />
          </button>
        </header>
        <ng-content />
      </section>
    }
  `,
})
export class TlSheet {
  readonly open = model(false);
  readonly title = input.required<string>();
  readonly closeLabel = input('Zamknij');

  protected close(): void {
    this.open.set(false);
  }
}
