import { Component } from '@angular/core';
import { TlCard, TlPageHeader, TlPanel, TlRing, TlSectionHeader, TlStat } from '@talia/ui';
import { TranslatePipe } from '../../core/i18n/i18n';

/** Weekly workouts + daily habits. Wired to the API in the activity/habits steps. */
@Component({
  selector: 'app-week',
  imports: [TlCard, TlPageHeader, TlPanel, TlRing, TlSectionHeader, TlStat, TranslatePipe],
  template: `
    <tl-page-header [title]="'week.title' | t" />

    <tl-panel [title]="'week.panel' | t">
      <div class="flex items-center justify-between">
        <tl-stat class="w-20" value="—" [label]="'week.avgSteps' | t" />
        <tl-ring [value]="0" [max]="3" [size]="150" [stroke]="12">
          <span class="text-4xl font-semibold tabular-nums">0</span>
          <span class="text-muted-foreground text-xs">{{ 'week.of' | t: { goal: 3 } }}</span>
        </tl-ring>
        <tl-stat class="w-20" value="—" [label]="'week.habits' | t" />
      </div>
    </tl-panel>

    <tl-section-header [title]="'week.workoutsList' | t" />
    <tl-card>
      <p class="text-muted-foreground text-sm">{{ 'week.noWorkouts' | t }}</p>
    </tl-card>

    <tl-section-header [title]="'week.habitsToday' | t" />
    <tl-card>
      <p class="text-muted-foreground text-sm">{{ 'week.habitsEmpty' | t }}</p>
    </tl-card>
  `,
})
export class WeekPage {}
