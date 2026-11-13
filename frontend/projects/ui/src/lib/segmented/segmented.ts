import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

export interface TlSegmentOption<T extends string = string> {
  value: T;
  label: string;
}

/** Pill segmented control. `<tl-segmented [options]="opts" [(value)]="mode" />` */
@Component({
  selector: 'tl-segmented',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    role: 'radiogroup',
    class: 'bg-chip grid w-full gap-1 rounded-full p-1',
    '[style.grid-template-columns]': '"repeat(" + options().length + ", minmax(0, 1fr))"',
  },
  template: `
    @for (option of options(); track option.value) {
      <button
        type="button"
        role="radio"
        [attr.aria-checked]="option.value === value()"
        class="cursor-pointer rounded-full px-3 py-2.5 text-sm font-medium transition hover:scale-[1.03]"
        [class]="
          option.value === value()
            ? 'bg-primary text-primary-foreground shadow-sm'
            : 'text-muted-foreground'
        "
        (click)="value.set(option.value)"
      >
        {{ option.label }}
      </button>
    }
  `,
})
export class TlSegmented {
  readonly options = input.required<readonly TlSegmentOption[]>();
  readonly value = model<string>();
}
