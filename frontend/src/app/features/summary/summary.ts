import { Component } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideSparkles } from '@ng-icons/lucide';
import { HlmCardImports } from '@ui/card';
import { TranslatePipe } from '../../core/i18n/i18n';

/** AI weekly summary - planned for v2, shows a "coming soon" card for now. */
@Component({
  selector: 'app-summary',
  imports: [HlmCardImports, NgIcon, TranslatePipe],
  viewProviders: [provideIcons({ lucideSparkles })],
  template: `
    <h1 class="mb-4 text-2xl font-bold">{{ 'summary.title' | t }}</h1>

    <section hlmCard class="bg-violet-500/8 ring-violet-500/20">
      <div hlmCardContent class="flex flex-col gap-2">
        <span class="flex items-center gap-1.5 font-semibold text-violet-600 dark:text-violet-400">
          <ng-icon name="lucideSparkles" />
          {{ 'summary.soonTitle' | t }}
        </span>
        <p class="leading-relaxed">{{ 'summary.soonBody' | t }}</p>
      </div>
    </section>
  `,
})
export class SummaryPage {}
