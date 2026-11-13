import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Screen title (e.g. today's date) with round action buttons projected on the right. */
@Component({
  selector: 'tl-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'mb-4 flex min-h-11 items-center justify-between gap-3' },
  template: `
    <div class="min-w-0">
      <h1 class="truncate text-xl font-semibold first-letter:uppercase">{{ title() }}</h1>
      @if (subtitle(); as sub) {
        <p class="text-muted-foreground text-sm">{{ sub }}</p>
      }
    </div>
    <div class="flex shrink-0 items-center gap-2">
      <ng-content />
    </div>
  `,
})
export class TlPageHeader {
  readonly title = input.required<string>();
  readonly subtitle = input<string | null>(null);
}
