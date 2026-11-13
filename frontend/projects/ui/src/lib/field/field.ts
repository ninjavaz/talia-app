import { ChangeDetectionStrategy, Component, Directive, input } from '@angular/core';

/** Large rounded input matching the cards. `<input tlInput inputmode="decimal" />` */
@Directive({
  selector: 'input[tlInput], select[tlInput]',
  host: {
    class:
      'bg-background text-foreground placeholder:text-muted-foreground focus-visible:ring-ring/50 h-12 w-full min-w-0 rounded-2xl border px-4 text-base outline-none focus-visible:ring-3 disabled:opacity-50 aria-invalid:border-destructive',
  },
})
export class TlInput {}

/** Label + control + optional unit and hint. Wrap an `input[tlInput]`. */
@Component({
  selector: 'tl-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <label class="flex flex-col gap-1.5">
      <span class="text-sm font-medium">{{ label() }}</span>
      <span class="relative flex items-center">
        <ng-content />
        @if (unit(); as u) {
          <span class="text-muted-foreground pointer-events-none absolute right-4 text-sm">{{ u }}</span>
        }
      </span>
      @if (hint(); as h) {
        <span class="text-muted-foreground text-xs">{{ h }}</span>
      }
    </label>
  `,
})
export class TlField {
  readonly label = input.required<string>();
  readonly unit = input<string | null>(null);
  readonly hint = input<string | null>(null);
}
