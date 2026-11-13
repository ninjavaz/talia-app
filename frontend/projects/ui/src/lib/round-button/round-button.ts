import { Directive, input } from '@angular/core';

const BASE =
  'inline-flex shrink-0 items-center justify-center rounded-full transition active:scale-95 disabled:pointer-events-none disabled:opacity-40';

const VARIANTS = {
  chip: 'bg-chip text-chip-foreground',
  solid: 'bg-primary text-primary-foreground',
  ghost: 'text-foreground hover:bg-chip',
} as const;

const SIZES = {
  sm: 'size-8 text-base',
  md: 'size-11 text-xl',
} as const;

/** Round icon button (header actions, row actions). `<button tlRoundButton size="sm"><ng-icon .../></button>` */
@Directive({
  selector: 'button[tlRoundButton], a[tlRoundButton]',
  host: { '[class]': 'classes()' },
})
export class TlRoundButton {
  readonly variant = input<keyof typeof VARIANTS>('chip');
  readonly size = input<keyof typeof SIZES>('md');

  protected classes(): string {
    return `${BASE} ${VARIANTS[this.variant()]} ${SIZES[this.size()]}`;
  }
}
