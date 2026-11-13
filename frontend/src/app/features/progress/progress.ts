import { Component } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucidePlus } from '@ng-icons/lucide';
import { HlmButtonImports } from '@ui/button';
import { HlmCardImports } from '@ui/card';
import { HlmSeparatorImports } from '@ui/separator';
import { TranslatePipe } from '../../core/i18n/i18n';

/** Waist + weight trend overview. Data comes with the measurements API (next step). */
@Component({
  selector: 'app-progress',
  imports: [HlmButtonImports, HlmCardImports, HlmSeparatorImports, NgIcon, TranslatePipe],
  viewProviders: [provideIcons({ lucidePlus })],
  template: `
    <h1 class="mb-4 text-2xl font-bold">{{ 'progress.title' | t }}</h1>

    <div class="grid grid-cols-2 gap-3">
      <section hlmCard size="sm" class="bg-primary/5 ring-primary/15">
        <div hlmCardContent class="flex flex-col gap-1">
          <span class="text-muted-foreground text-xs">{{ 'progress.waist' | t }}</span>
          <span class="text-2xl font-semibold">—</span>
          <span class="text-muted-foreground text-xs">{{ 'progress.noData' | t }}</span>
        </div>
      </section>
      <section hlmCard size="sm" class="bg-primary/5 ring-primary/15">
        <div hlmCardContent class="flex flex-col gap-1">
          <span class="text-muted-foreground text-xs">{{ 'progress.weightTrend' | t }}</span>
          <span class="text-2xl font-semibold">—</span>
          <span class="text-muted-foreground text-xs">{{ 'progress.noData' | t }}</span>
        </div>
      </section>
    </div>

    <div
      class="text-muted-foreground my-5 flex h-44 items-center justify-center rounded-xl border border-dashed px-6 text-center text-sm"
    >
      {{ 'progress.chartEmpty' | t }}
    </div>

    <button hlmBtn size="lg" class="h-11 w-full text-base" disabled>
      <ng-icon name="lucidePlus" />
      {{ 'progress.addWaist' | t }}
    </button>

    <div class="mt-5 flex items-center justify-between py-2 text-sm">
      <span>{{ 'progress.stepsToday' | t }}</span>
      <span class="font-semibold">—</span>
    </div>
    <hlm-separator />
  `,
})
export class ProgressPage {}
