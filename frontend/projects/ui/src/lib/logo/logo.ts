import { ChangeDetectionStrategy, Component, booleanAttribute, computed, input, numberAttribute } from '@angular/core';

/**
 * Talia logo: two curves forming a waist silhouette )( with a small diamond at the narrowest point,
 * on a charcoal tile. Colours follow the theme tokens. `wordmark` adds the name next to the mark.
 *
 * <tl-logo [size]="64" wordmark />
 */
@Component({
  selector: 'tl-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'inline-flex items-center',
    '[style.gap.px]': 'size() * 0.28',
    '[attr.role]': '"img"',
    '[attr.aria-label]': 'label()',
  },
  template: `
    <svg [attr.width]="size()" [attr.height]="size()" viewBox="0 0 64 64" aria-hidden="true">
      <rect width="64" height="64" rx="18" class="fill-panel" />
      <path
        d="M21 13 C31 25 31 39 21 51"
        fill="none"
        class="stroke-progress"
        stroke-width="4.5"
        stroke-linecap="round"
      />
      <path
        d="M43 13 C33 25 33 39 43 51"
        fill="none"
        class="stroke-progress"
        stroke-width="4.5"
        stroke-linecap="round"
      />
      <path d="M32 27.6 L34.7 32 L32 36.4 L29.3 32 Z" class="fill-panel-foreground" />
    </svg>
    @if (wordmark()) {
      <span class="leading-none font-semibold tracking-tight" [style.font-size.px]="fontSize()" aria-hidden="true">
        {{ label() }}
      </span>
    }
  `,
})
export class TlLogo {
  readonly size = input(40, { transform: numberAttribute });
  readonly wordmark = input(false, { transform: booleanAttribute });
  readonly label = input('Talia');

  protected readonly fontSize = computed(() => Math.round(this.size() * 0.72));
}
