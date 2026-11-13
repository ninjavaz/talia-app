import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideCalendarCheck, lucideSettings, lucideSparkles, lucideTrendingDown } from '@ng-icons/lucide';
import { TranslatePipe } from '../core/i18n/i18n';
import { TranslationKey } from '../core/i18n/translations';

interface NavItem {
  path: string;
  label: TranslationKey;
  icon: string;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgIcon, TranslatePipe],
  viewProviders: [provideIcons({ lucideTrendingDown, lucideCalendarCheck, lucideSparkles, lucideSettings })],
  template: `
    <main
      class="mx-auto w-full max-w-md px-4 pt-[calc(env(safe-area-inset-top)+1.25rem)] pb-[calc(env(safe-area-inset-bottom)+5.5rem)]"
    >
      <router-outlet />
    </main>

    <nav
      class="bg-background/90 fixed inset-x-0 bottom-0 border-t backdrop-blur pb-[env(safe-area-inset-bottom)]"
    >
      <ul class="mx-auto grid max-w-md grid-cols-4">
        @for (item of nav; track item.path) {
          <li>
            <a
              [routerLink]="item.path"
              routerLinkActive="text-primary"
              class="text-muted-foreground flex flex-col items-center gap-1 py-2.5 text-[11px] font-medium"
            >
              <ng-icon [name]="item.icon" class="text-xl" />
              {{ item.label | t }}
            </a>
          </li>
        }
      </ul>
    </nav>
  `,
})
export class Shell {
  protected readonly nav: NavItem[] = [
    { path: 'progress', label: 'nav.progress', icon: 'lucideTrendingDown' },
    { path: 'week', label: 'nav.week', icon: 'lucideCalendarCheck' },
    { path: 'summary', label: 'nav.summary', icon: 'lucideSparkles' },
    { path: 'settings', label: 'nav.settings', icon: 'lucideSettings' },
  ];
}
