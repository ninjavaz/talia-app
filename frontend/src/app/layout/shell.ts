import { Component, computed, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TlBottomNav, TlNavItem } from '@talia/ui';
import { I18n } from '../core/i18n/i18n';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, TlBottomNav],
  template: `
    <main
      class="mx-auto w-full max-w-md px-4 pt-[calc(env(safe-area-inset-top)+1rem)] pb-[calc(env(safe-area-inset-bottom)+6.5rem)]"
    >
      <router-outlet />
    </main>
    <tl-bottom-nav [items]="nav()" />
  `,
})
export class Shell {
  private readonly i18n = inject(I18n);

  protected readonly nav = computed<TlNavItem[]>(() => {
    this.i18n.lang();
    return [
      { path: 'progress', label: this.i18n.t('nav.progress'), icon: 'lucideTrendingDown' },
      { path: 'week', label: this.i18n.t('nav.week'), icon: 'lucideCalendarCheck' },
      { path: 'summary', label: this.i18n.t('nav.summary'), icon: 'lucideSparkles' },
      { path: 'settings', label: this.i18n.t('nav.settings'), icon: 'lucideSettings' },
    ];
  });
}
