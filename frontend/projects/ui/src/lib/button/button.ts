import { Directive, input } from '@angular/core';

const BASE =
  'inline-flex h-12 cursor-pointer items-center justify-center gap-2 rounded-full px-5 text-base font-medium transition duration-200 hover:scale-[1.02] hover:shadow-md active:scale-[0.98] disabled:pointer-events-none disabled:opacity-50';

const VARIANTS = {
  primary: 'bg-primary text-primary-foreground',
  secondary: 'bg-chip text-chip-foreground',
  danger: 'bg-destructive/10 text-destructive',
  accent: 'bg-progress text-white',
} as const;

/** Big pill button. `<button tlButton variant="secondary">Anuluj</button>` */
@Directive({
  selector: 'button[tlButton], a[tlButton]',
  host: { '[class]': 'classes()' },
})
export class TlButton {
  readonly variant = input<keyof typeof VARIANTS>('primary');

  protected classes(): string {
    return `${BASE} ${VARIANTS[this.variant()]}`;
  }
}
