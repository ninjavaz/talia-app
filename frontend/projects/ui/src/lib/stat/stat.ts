import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type TlStatTone = 'muted' | 'success' | 'danger';

/** Big number with a caption and an optional hint line (e.g. "▼ 1,8 kg"). */
@Component({
  selector: 'tl-stat',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-col items-center text-center' },
  template: `
    <span class="text-[1.75rem] leading-none font-semibold tabular-nums">
      {{ value() }}
      @if (unit(); as u) {
        <span class="text-muted-foreground text-base font-medium">{{ u }}</span>
      }
    </span>
    <span class="text-muted-foreground mt-1.5 text-xs">{{ label() }}</span>
    @if (hint(); as h) {
      <span
        class="mt-1 text-xs font-semibold"
        [class.text-success]="tone() === 'success'"
        [class.text-destructive]="tone() === 'danger'"
        [class.text-muted-foreground]="tone() === 'muted'"
        >{{ h }}</span
      >
    }
  `,
})
export class TlStat {
  readonly value = input.required<string | number>();
  readonly label = input.required<string>();
  readonly unit = input<string | null>(null);
  readonly hint = input<string | null>(null);
  readonly tone = input<TlStatTone>('muted');
}
