import { ChangeDetectionStrategy, Component, booleanAttribute, input } from '@angular/core';

/** White rounded surface. `padded="false"` for lists that manage their own padding. */
@Component({
  selector: 'tl-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'bg-card text-card-foreground block rounded-[1.75rem] shadow-sm',
    '[class.p-5]': 'padded()',
  },
  template: `<ng-content />`,
})
export class TlCard {
  readonly padded = input(true, { transform: booleanAttribute });
}
