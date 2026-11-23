import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterRenderEffect,
  inject,
  input,
  model,
} from '@angular/core';
import { TlSegmentOption } from '../segmented/segmented';

/**
 * Single-choice row of pill chips that scrolls sideways instead of squeezing its labels,
 * for option lists of any length (e.g. one chip per body part).
 * `<tl-chip-select [options]="opts" [(value)]="metric" />`
 */
@Component({
  selector: 'tl-chip-select',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    role: 'radiogroup',
    class:
      'flex w-full snap-x gap-2 overflow-x-auto overscroll-x-contain [scrollbar-width:none] [&::-webkit-scrollbar]:hidden',
  },
  template: `
    @for (option of options(); track option.value) {
      <button
        type="button"
        role="radio"
        [attr.aria-checked]="option.value === value()"
        class="max-w-[70%] shrink-0 cursor-pointer snap-start truncate rounded-full px-4 py-2 text-sm font-medium whitespace-nowrap transition hover:scale-[1.03]"
        [class]="
          option.value === value()
            ? 'bg-primary text-primary-foreground shadow-sm'
            : 'bg-chip text-muted-foreground'
        "
        [title]="option.label"
        (click)="value.set(option.value)"
      >
        {{ option.label }}
      </button>
    }
  `,
})
export class TlChipSelect {
  readonly options = input.required<readonly TlSegmentOption[]>();
  readonly value = model<string>();

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  constructor() {
    // keep the selected chip visible, also when it is chosen from outside the row
    afterRenderEffect(() => {
      this.value();
      this.options();
      const row = this.host.nativeElement;
      const chip = row.querySelector<HTMLElement>('[aria-checked="true"]');
      if (!chip) {
        return;
      }
      const left = chip.offsetLeft - row.offsetLeft;
      const right = left + chip.offsetWidth;
      if (left < row.scrollLeft) {
        row.scrollTo?.({ left, behavior: 'smooth' });
      } else if (right > row.scrollLeft + row.clientWidth) {
        row.scrollTo?.({ left: right - row.clientWidth, behavior: 'smooth' });
      }
    });
  }
}
