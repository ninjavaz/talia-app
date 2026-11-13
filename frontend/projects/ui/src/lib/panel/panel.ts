import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideChevronRight } from '@ng-icons/lucide';

/**
 * Hero card: dark header band with a title (optionally a link with a chevron) and a white body on top of it.
 *
 * <tl-panel title="Postęp" link="/progress">...</tl-panel>
 */
@Component({
  selector: 'tl-panel',
  imports: [RouterLink, NgIcon],
  viewProviders: [provideIcons({ lucideChevronRight })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block overflow-hidden rounded-[1.75rem] bg-panel shadow-sm' },
  template: `
    @if (link(); as target) {
      <a
        [routerLink]="target"
        class="text-panel-foreground flex items-center justify-between px-5 pt-3.5 pb-3 font-semibold"
      >
        {{ title() }}
        <ng-icon name="lucideChevronRight" class="text-lg" />
      </a>
    } @else {
      <div class="text-panel-foreground px-5 pt-3.5 pb-3 font-semibold">{{ title() }}</div>
    }
    <div class="bg-card text-card-foreground rounded-t-[1.75rem] p-5">
      <ng-content />
    </div>
  `,
})
export class TlPanel {
  readonly title = input.required<string>();
  readonly link = input<string | readonly unknown[] | null>(null);
}
