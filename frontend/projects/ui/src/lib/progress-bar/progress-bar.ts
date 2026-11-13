import { ChangeDetectionStrategy, Component, computed, input, numberAttribute } from '@angular/core';

/** Thin rounded progress bar in the progress (sage) colour. */
@Component({
  selector: 'tl-progress-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    role: 'progressbar',
    class: 'bg-track block h-2 w-full overflow-hidden rounded-full',
    '[attr.aria-valuemin]': '0',
    '[attr.aria-valuemax]': 'max()',
    '[attr.aria-valuenow]': 'value()',
  },
  template: `
    <div
      class="bg-progress h-full rounded-full transition-[width] duration-700 ease-out"
      [style.width.%]="percent()"
    ></div>
  `,
})
export class TlProgressBar {
  readonly value = input(0, { transform: numberAttribute });
  readonly max = input(100, { transform: numberAttribute });

  protected readonly percent = computed(() =>
    this.max() > 0 ? Math.min(Math.max((this.value() / this.max()) * 100, 0), 100) : 0,
  );
}
