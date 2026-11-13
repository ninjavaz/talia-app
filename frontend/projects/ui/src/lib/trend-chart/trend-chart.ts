import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface TlChartPoint {
  /** ISO date, e.g. 2026-11-15 */
  date: string;
  value: number;
}

const W = 320;
const H = 150;
const PAD = { top: 10, right: 34, bottom: 22, left: 4 };

/**
 * Measurements as dots plus a trend line. Needs at least 2 points, otherwise shows the projected empty state.
 *
 * <tl-trend-chart [points]="weights" [trend]="trend" [format]="fmt">Brak danych</tl-trend-chart>
 */
@Component({
  selector: 'tl-trend-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    @if (geometry(); as g) {
      <svg [attr.viewBox]="'0 0 ' + width + ' ' + height" class="h-auto w-full overflow-visible" role="img"
           [attr.aria-label]="ariaLabel()">
        @for (tick of g.ticks; track tick.y) {
          <line [attr.x1]="pad.left" [attr.x2]="width - pad.right" [attr.y1]="tick.y" [attr.y2]="tick.y"
                class="stroke-border" stroke-dasharray="3 4" />
          <text [attr.x]="width - pad.right + 6" [attr.y]="tick.y + 4" class="fill-muted-foreground text-[10px]">
            {{ tick.label }}
          </text>
        }
        @for (p of g.dots; track $index) {
          <circle [attr.cx]="p.x" [attr.cy]="p.y" r="3.5" class="fill-muted-foreground/35" />
        }
        @if (g.line) {
          <polyline [attr.points]="g.line" fill="none" class="stroke-progress" stroke-width="3"
                    stroke-linecap="round" stroke-linejoin="round" />
        }
        <text [attr.x]="pad.left" [attr.y]="height - 4" class="fill-muted-foreground text-[10px]">
          {{ g.firstLabel }}
        </text>
        <text [attr.x]="width - pad.right" [attr.y]="height - 4" text-anchor="end"
              class="fill-muted-foreground text-[10px]">{{ g.lastLabel }}</text>
      </svg>
    } @else {
      <div class="text-muted-foreground flex h-36 items-center justify-center rounded-2xl border border-dashed px-6 text-center text-sm">
        <ng-content />
      </div>
    }
  `,
})
export class TlTrendChart {
  readonly points = input.required<readonly TlChartPoint[]>();
  readonly trend = input<readonly TlChartPoint[]>([]);
  /** Formats axis values (e.g. Polish decimal comma). */
  readonly format = input<(value: number) => string>((v) => v.toFixed(1));
  /** Formats the first/last date under the chart. */
  readonly formatDate = input<(isoDate: string) => string>((d) => d.slice(8, 10) + '.' + d.slice(5, 7));
  readonly ariaLabel = input('');

  protected readonly width = W;
  protected readonly height = H;
  protected readonly pad = PAD;

  protected readonly geometry = computed(() => {
    const points = this.points();
    if (points.length < 2) {
      return null;
    }
    const trend = this.trend().length >= 2 ? this.trend() : points;
    const all = [...points, ...trend];
    const times = all.map((p) => Date.parse(p.date));
    const t0 = Math.min(...times);
    const t1 = Math.max(...times);
    let lo = Math.min(...all.map((p) => p.value));
    let hi = Math.max(...all.map((p) => p.value));
    const span = Math.max(hi - lo, 1);
    lo -= span * 0.15;
    hi += span * 0.15;

    const x = (date: string) =>
      PAD.left + ((Date.parse(date) - t0) / Math.max(t1 - t0, 1)) * (W - PAD.left - PAD.right);
    const y = (value: number) => PAD.top + (1 - (value - lo) / (hi - lo)) * (H - PAD.top - PAD.bottom);
    const fmt = this.format();

    return {
      dots: points.map((p) => ({ x: x(p.date), y: y(p.value) })),
      line: trend.map((p) => `${x(p.date).toFixed(1)},${y(p.value).toFixed(1)}`).join(' '),
      ticks: [hi - (hi - lo) * 0.15, (hi + lo) / 2, lo + (hi - lo) * 0.15].map((v) => ({ y: y(v), label: fmt(v) })),
      firstLabel: this.formatDate()(points[0].date),
      lastLabel: this.formatDate()(points[points.length - 1].date),
    };
  });
}
