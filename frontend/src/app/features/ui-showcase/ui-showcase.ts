import { Component, inject, signal } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import {
  TlBottomNav,
  TlCard,
  TlCheck,
  TlList,
  TlListItem,
  TlPageHeader,
  TlPanel,
  TlProgressBar,
  TlRing,
  TlRoundButton,
  TlSectionHeader,
  TlSegmented,
  TlStat,
} from '@talia/ui';
import { Theme, ThemeMode } from '../../core/theme/theme';

/** Living style guide of @talia/ui with sample data. Public route /ui (no login, no API calls). */
@Component({
  selector: 'app-ui-showcase',
  imports: [
    NgIcon,
    TlBottomNav,
    TlCard,
    TlCheck,
    TlList,
    TlListItem,
    TlPageHeader,
    TlPanel,
    TlProgressBar,
    TlRing,
    TlRoundButton,
    TlSectionHeader,
    TlSegmented,
    TlStat,
  ],
  template: `
    <main
      class="mx-auto w-full max-w-md px-4 pt-[calc(env(safe-area-inset-top)+1rem)] pb-[calc(env(safe-area-inset-bottom)+6.5rem)]"
    >
      <tl-page-header title="piątek, 13.11">
        <button tlRoundButton aria-label="Dodaj"><ng-icon name="lucidePlus" /></button>
        <button tlRoundButton aria-label="Kalendarz"><ng-icon name="lucideCalendarCheck" /></button>
      </tl-page-header>

      <tl-panel title="Postęp" link="/ui">
        <div class="grid grid-cols-2">
          <tl-stat value="78" unit="cm" label="Talia" hint="▼ 3 cm od startu" tone="success" />
          <tl-stat value="64,2" unit="kg" label="Waga (trend)" hint="▼ 1,8 kg" tone="success" />
        </div>
      </tl-panel>

      <tl-section-header title="Dziś" link="/ui" />
      <tl-card>
        <div class="flex justify-around">
          @for (r of rings; track r.label) {
            <div class="flex flex-col items-center gap-2">
              <tl-ring [value]="r.value" [max]="r.max" [size]="76">
                <span class="font-semibold">{{ r.text }}</span>
              </tl-ring>
              <span class="text-muted-foreground text-xs">{{ r.label }}</span>
            </div>
          }
        </div>
      </tl-card>

      <tl-section-header title="Ten tydzień" />
      <tl-panel title="Treningi">
        <div class="flex items-center justify-between">
          <tl-stat class="w-20" value="8,1k" label="Kroki śr." />
          <tl-ring [value]="2" [max]="3" [size]="150" [stroke]="12">
            <span class="text-4xl font-semibold">2</span>
            <span class="text-muted-foreground text-xs">z 3</span>
          </tl-ring>
          <tl-stat class="w-20" value="78%" label="Nawyki" />
        </div>
        <tl-progress-bar class="mt-5" [value]="2" [max]="3" />
      </tl-panel>

      <tl-section-header title="Nawyki dziś" />
      <tl-list>
        @for (h of habits(); track h.name; let i = $index) {
          <tl-list-item [title]="h.name" [subtitle]="h.done ? 'zrobione' : null">
            <button tlListTrailing tlCheck [checked]="h.done" (click)="toggle(i)"></button>
          </tl-list-item>
        }
      </tl-list>

      <tl-section-header title="Pomiary" />
      <tl-list>
        <tl-list-item icon="lucideRuler" title="Talia" subtitle="78 cm · niedziela">
          <button tlListTrailing tlRoundButton size="sm"><ng-icon name="lucidePlus" /></button>
        </tl-list-item>
        <tl-list-item icon="lucideScale" title="Waga" subtitle="64,0 kg · dziś 7:12">
          <button tlListTrailing tlRoundButton size="sm"><ng-icon name="lucideChevronRight" /></button>
        </tl-list-item>
        <tl-list-item icon="lucideDumbbell" title="Siłownia" subtitle="wtorek · 65 min">
          <span tlListTrailing class="text-muted-foreground text-sm">310 kcal</span>
        </tl-list-item>
      </tl-list>

      <tl-section-header title="Motyw" />
      <tl-card>
        <tl-segmented [options]="themeOptions" [value]="theme.mode()" (valueChange)="setTheme($event)" />
      </tl-card>
    </main>

    <tl-bottom-nav [items]="nav" />
  `,
})
export class UiShowcasePage {
  protected readonly theme = inject(Theme);

  protected readonly rings = [
    { label: 'Kroki', value: 7432, max: 8000, text: '7,4k' },
    { label: 'Treningi', value: 2, max: 3, text: '2/3' },
    { label: 'Nawyki', value: 2, max: 3, text: '67%' },
  ];

  protected readonly habits = signal([
    { name: 'Białko w posiłkach', done: true },
    { name: 'Warzywa do 2 posiłków', done: true },
    { name: 'Bez słodkich napojów', done: false },
  ]);

  protected readonly themeOptions = [
    { value: 'system', label: 'Auto' },
    { value: 'light', label: 'Jasny' },
    { value: 'dark', label: 'Ciemny' },
  ];

  protected readonly nav = [
    { path: '/ui', label: 'Postęp', icon: 'lucideTrendingDown' },
    { path: '/ui/week', label: 'Tydzień', icon: 'lucideCalendarCheck' },
    { path: '/ui/summary', label: 'Podsumowanie', icon: 'lucideSparkles' },
    { path: '/ui/settings', label: 'Ustawienia', icon: 'lucideSettings' },
  ];

  protected toggle(index: number): void {
    this.habits.update((list) => list.map((h, i) => (i === index ? { ...h, done: !h.done } : h)));
  }

  protected setTheme(value: string | undefined): void {
    if (value === 'system' || value === 'light' || value === 'dark') {
      this.theme.setMode(value as ThemeMode);
    }
  }
}
