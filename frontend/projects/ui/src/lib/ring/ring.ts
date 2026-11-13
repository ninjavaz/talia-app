import { ChangeDetectionStrategy, Component, computed, input, numberAttribute } from '@angular/core';

/**
 * Circular progress gauge. By default a 270° arc open at the bottom; `arc="360"` gives a full circle.
 * Anything projected is centred inside the ring (value, unit, caption).
 *
 * <tl-ring [value]="7432" [max]="8000" [size]="80"><b>7,4k</b></tl-ring>
 */
@Component({
  selector: 'tl-ring',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'relative inline-flex shrink-0 items-center justify-center',
    role: 'progressbar',
    '[attr.aria-valuemin]': '0',
    '[attr.aria-valuemax]': 'max()',
    '[attr.aria-valuenow]': 'value()',
    '[style.width.px]': 'size()',
    '[style.height.px]': 'size()',
  },
  template: `
    <svg class="absolute inset-0 size-full" [attr.viewBox]="viewBox()" aria-hidden="true">
      <circle
        class="stroke-track"
        fill="none"
        stroke-linecap="round"
        [attr.cx]="center()"
        [attr.cy]="center()"
        [attr.r]="radius()"
        [attr.stroke-width]="stroke()"
        [attr.stroke-dasharray]="trackDash()"
        [attr.transform]="rotation()"
      />
      <circle
        class="stroke-progress transition-[stroke-dasharray] duration-700 ease-out"
        fill="none"
        stroke-linecap="round"
        [style.opacity]="fraction() > 0 ? 1 : 0"
        [attr.cx]="center()"
        [attr.cy]="center()"
        [attr.r]="radius()"
        [attr.stroke-width]="stroke()"
        [attr.stroke-dasharray]="valueDash()"
        [attr.transform]="rotation()"
      />
    </svg>
    <div class="relative flex flex-col items-center justify-center text-center leading-tight">
      <ng-content />
    </div>
  `,
})
export class TlRing {
  readonly value = input(0, { transform: numberAttribute });
  readonly max = input(100, { transform: numberAttribute });
  readonly size = input(72, { transform: numberAttribute });
  readonly stroke = input(6, { transform: numberAttribute });
  /** Length of the arc in degrees (270 = gauge open at the bottom, 360 = full circle). */
  readonly arc = input(270, { transform: numberAttribute });

  protected readonly viewBox = computed(() => `0 0 ${this.size()} ${this.size()}`);
  protected readonly center = computed(() => this.size() / 2);
  protected readonly radius = computed(() => (this.size() - this.stroke()) / 2);

  protected readonly fraction = computed(() => {
    const max = this.max();
    return max > 0 ? Math.min(Math.max(this.value() / max, 0), 1) : 0;
  });

  private readonly circumference = computed(() => 2 * Math.PI * this.radius());
  private readonly arcLength = computed(() => (this.circumference() * this.arc()) / 360);

  protected readonly trackDash = computed(() => `${this.arcLength()} ${this.circumference()}`);
  protected readonly valueDash = computed(
    () => `${this.arcLength() * this.fraction()} ${this.circumference()}`,
  );
  /** Rotate so the gap of the arc is centred at the bottom. */
  protected readonly rotation = computed(
    () => `rotate(${90 + (360 - this.arc()) / 2} ${this.center()} ${this.center()})`,
  );
}
