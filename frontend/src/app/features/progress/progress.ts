import { Component, inject } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import {
  TlCard,
  TlList,
  TlListItem,
  TlPageHeader,
  TlPanel,
  TlRing,
  TlRoundButton,
  TlSectionHeader,
  TlStat,
} from '@talia/ui';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';

/** Waist + weight trend overview. Values arrive with the measurements API (next step). */
@Component({
  selector: 'app-progress',
  imports: [
    NgIcon,
    TlCard,
    TlList,
    TlListItem,
    TlPageHeader,
    TlPanel,
    TlRing,
    TlRoundButton,
    TlSectionHeader,
    TlStat,
    TranslatePipe,
  ],
  template: `
    <tl-page-header [title]="i18n.today()">
      <button tlRoundButton disabled [attr.aria-label]="'progress.addWaist' | t">
        <ng-icon name="lucidePlus" />
      </button>
    </tl-page-header>

    <tl-panel [title]="'progress.panel' | t">
      <div class="grid grid-cols-2">
        <tl-stat value="—" [label]="'progress.waist' | t" [hint]="'common.noData' | t" />
        <tl-stat value="—" [label]="'progress.weightTrend' | t" [hint]="'common.noData' | t" />
      </div>
      <div
        class="border-border text-muted-foreground mt-5 flex h-36 items-center justify-center rounded-2xl border border-dashed px-6 text-center text-sm"
      >
        {{ 'progress.chartEmpty' | t }}
      </div>
    </tl-panel>

    <tl-section-header [title]="'progress.today' | t" link="/week" />
    <tl-card>
      <div class="flex justify-around">
        <div class="flex flex-col items-center gap-2">
          <tl-ring [value]="0" [max]="8000" [size]="76"><span class="font-semibold">—</span></tl-ring>
          <span class="text-muted-foreground text-xs">{{ 'progress.steps' | t }}</span>
        </div>
        <div class="flex flex-col items-center gap-2">
          <tl-ring [value]="0" [max]="3" [size]="76"><span class="font-semibold">0/3</span></tl-ring>
          <span class="text-muted-foreground text-xs">{{ 'progress.workouts' | t }}</span>
        </div>
        <div class="flex flex-col items-center gap-2">
          <tl-ring [value]="0" [max]="100" [size]="76"><span class="font-semibold">—</span></tl-ring>
          <span class="text-muted-foreground text-xs">{{ 'progress.habits' | t }}</span>
        </div>
      </div>
    </tl-card>

    <tl-section-header [title]="'progress.measurements' | t" />
    <tl-list>
      <tl-list-item icon="lucideRuler" [title]="'progress.waist' | t" [subtitle]="'progress.waistHint' | t">
        <button tlListTrailing tlRoundButton size="sm" disabled [attr.aria-label]="'progress.addWaist' | t">
          <ng-icon name="lucidePlus" />
        </button>
      </tl-list-item>
      <tl-list-item icon="lucideScale" [title]="'progress.weightTrend' | t" [subtitle]="'progress.weightHint' | t" />
    </tl-list>
  `,
})
export class ProgressPage {
  protected readonly i18n = inject(I18n);
}
