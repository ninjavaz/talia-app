import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideChevronRight } from '@ng-icons/lucide';

/** Bold section title above a group of cards, optionally a link with a chevron. */
@Component({
  selector: 'tl-section-header',
  imports: [RouterLink, NgIcon],
  viewProviders: [provideIcons({ lucideChevronRight })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'mt-7 mb-3 block' },
  template: `
    @if (link(); as target) {
      <a [routerLink]="target" class="flex items-center justify-between">
        <h2 class="text-xl font-bold">{{ title() }}</h2>
        <ng-icon name="lucideChevronRight" class="text-xl" />
      </a>
    } @else {
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-bold">{{ title() }}</h2>
        <ng-content />
      </div>
    }
  `,
})
export class TlSectionHeader {
  readonly title = input.required<string>();
  readonly link = input<string | readonly unknown[] | null>(null);
}
