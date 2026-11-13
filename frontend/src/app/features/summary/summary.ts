import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { TlList, TlListItem, TlPageHeader, TlPanel, TlSectionHeader } from '@talia/ui';
import { TranslatePipe } from '../../core/i18n/i18n';

/** AI weekly summary - planned for v2, shows a "coming soon" panel for now. */
@Component({
  selector: 'app-summary',
  imports: [NgIcon, TlList, TlListItem, TlPageHeader, TlPanel, TlSectionHeader, TranslatePipe],
  template: `
    <tl-page-header [title]="'summary.title' | t" />

    <tl-panel [title]="'summary.panel' | t">
      <p class="text-progress flex items-center gap-1.5 font-semibold">
        <ng-icon name="lucideSparkles" />
        {{ 'summary.soonTitle' | t }}
      </p>
      <p class="mt-2 leading-relaxed">{{ 'summary.soonBody' | t }}</p>
    </tl-panel>

    <tl-section-header [title]="'summary.numbers' | t" />
    <tl-list>
      <tl-list-item icon="lucideRuler" [title]="'progress.waist' | t">
        <span tlListTrailing class="font-semibold">—</span>
      </tl-list-item>
      <tl-list-item icon="lucideScale" [title]="'progress.weightTrend' | t">
        <span tlListTrailing class="font-semibold">—</span>
      </tl-list-item>
      <tl-list-item icon="lucideDumbbell" [title]="'progress.workouts' | t">
        <span tlListTrailing class="font-semibold">—</span>
      </tl-list-item>
      <tl-list-item icon="lucideListChecks" [title]="'progress.habits' | t">
        <span tlListTrailing class="font-semibold">—</span>
      </tl-list-item>
    </tl-list>
  `,
})
export class SummaryPage {}
