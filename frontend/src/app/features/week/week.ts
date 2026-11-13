import { Component } from '@angular/core';
import { HlmProgressImports } from '@ui/progress';
import { TranslatePipe } from '../../core/i18n/i18n';

/** Weekly workouts + daily habits. Wired to the API in the activity/habits steps. */
@Component({
  selector: 'app-week',
  imports: [HlmProgressImports, TranslatePipe],
  template: `
    <h1 class="mb-4 text-2xl font-bold">{{ 'week.title' | t }}</h1>

    <p class="text-muted-foreground mb-2 text-sm">{{ 'week.workouts' | t: { done: 0, goal: 3 } }}</p>
    <hlm-progress [value]="0" class="h-2">
      <hlm-progress-indicator />
    </hlm-progress>
    <p class="text-muted-foreground mt-4 text-sm">{{ 'week.noWorkouts' | t }}</p>

    <h2 class="mt-8 mb-3 text-lg font-semibold">{{ 'week.habitsToday' | t }}</h2>
    <p class="text-muted-foreground text-sm">{{ 'week.habitsEmpty' | t }}</p>
  `,
})
export class WeekPage {}
