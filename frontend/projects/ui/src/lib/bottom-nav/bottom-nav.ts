import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgIcon } from '@ng-icons/core';

export interface TlNavItem {
  path: string;
  label: string;
  /** Registered ng-icon name, e.g. 'lucideSettings'. */
  icon: string;
}

/** Floating dark pill navigation at the bottom of the screen (respects the iPhone home indicator). */
@Component({
  selector: 'tl-bottom-nav',
  imports: [RouterLink, RouterLinkActive, NgIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class:
      'pointer-events-none fixed inset-x-0 bottom-0 z-40 block px-4 pb-[calc(env(safe-area-inset-bottom)+0.5rem)]',
  },
  template: `
    <nav
      class="bg-nav pointer-events-auto mx-auto grid max-w-md rounded-[1.75rem] p-1.5 shadow-xl shadow-black/20"
      [style.grid-template-columns]="'repeat(' + items().length + ', minmax(0, 1fr))'"
    >
      @for (item of items(); track item.path) {
        <a
          [routerLink]="item.path"
          routerLinkActive
          ariaCurrentWhenActive="page"
          class="text-nav-foreground aria-[current=page]:text-nav-active flex flex-col items-center gap-1 rounded-2xl py-2 text-[11px] font-medium transition-colors aria-[current=page]:bg-white/8 aria-[current=page]:font-semibold"
        >
          <ng-icon [name]="item.icon" class="text-[22px]" />
          {{ item.label }}
        </a>
      }
    </nav>
  `,
})
export class TlBottomNav {
  readonly items = input.required<readonly TlNavItem[]>();
}
