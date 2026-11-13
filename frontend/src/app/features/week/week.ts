import { httpResource } from '@angular/common/http';
import { Component, inject, linkedSignal, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import {
  TlCard,
  TlCheck,
  TlList,
  TlListItem,
  TlPageHeader,
  TlPanel,
  TlRing,
  TlRoundButton,
  TlSectionHeader,
  TlStat,
} from '@talia/ui';
import { Api, HabitDay, WeekView, Workout, isoDate, safeValue } from '../../core/api';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';
import { TranslationKey } from '../../core/i18n/translations';
import { WORKOUT_ICONS } from '../../core/icons';
import { WorkoutSheet } from './workout-sheet';

/** This week: workouts vs goal, today's habit checklist and the list of workouts. */
@Component({
  selector: 'app-week',
  imports: [
    NgIcon,
    RouterLink,
    TlCard,
    TlCheck,
    TlList,
    TlListItem,
    TlPageHeader,
    TlPanel,
    TlRing,
    TlRoundButton,
    TlSectionHeader,
    TlStat,
    TranslatePipe,
    WorkoutSheet,
  ],
  template: `
    @let w = weekData();
    <tl-page-header [title]="'week.title' | t" [subtitle]="w ? i18n.weekRange(w.weekStart, w.weekEnd) : null" />

    <tl-panel [title]="'week.panel' | t">
      <div class="flex items-center justify-between">
        <tl-stat class="w-20" [value]="i18n.compact(w?.avgDailySteps)" [label]="'week.avgSteps' | t" />
        <tl-ring [value]="w?.workoutsDone ?? 0" [max]="w?.workoutGoal || 1" [size]="150" [stroke]="12">
          <span class="text-4xl font-semibold tabular-nums">{{ w?.workoutsDone ?? 0 }}</span>
          <span class="text-muted-foreground text-xs">{{ 'week.of' | t: { goal: w?.workoutGoal ?? 0 } }}</span>
        </tl-ring>
        <tl-stat class="w-20" [value]="(w?.habits?.weekPct ?? 0) + '%'" [label]="'week.habits' | t" />
      </div>
    </tl-panel>

    <tl-section-header [title]="'week.habitsToday' | t" />
    @if (habits().length) {
      <tl-list>
        @for (h of habits(); track h.id) {
          <tl-list-item [title]="h.name">
            <button tlListTrailing tlCheck [checked]="h.done" [attr.aria-label]="h.name" (click)="toggle(h)"></button>
          </tl-list-item>
        }
      </tl-list>
      @if (w?.habits; as s) {
        <p class="text-muted-foreground mt-2 px-2 text-xs">
          {{ 'week.streak' | t: { weeks: s.streakWeeks, pct: s.thresholdPct } }}
        </p>
      }
    } @else if (!habitDay.isLoading()) {
      <a routerLink="/settings">
        <tl-card><p class="text-muted-foreground text-sm">{{ 'week.habitsEmpty' | t }}</p></tl-card>
      </a>
    }

    <tl-section-header [title]="'week.workoutsList' | t">
      <button tlRoundButton size="sm" [attr.aria-label]="'week.addWorkout' | t" (click)="workoutOpen.set(true)">
        <ng-icon name="lucidePlus" />
      </button>
    </tl-section-header>
    @if (w?.workouts?.length) {
      <tl-list>
        @for (x of w!.workouts; track x.id) {
          <tl-list-item [icon]="icons[x.category]" [title]="i18n.t(categoryKey(x))" [subtitle]="workoutSubtitle(x)">
            <button
              tlListTrailing
              tlRoundButton
              size="sm"
              variant="ghost"
              [attr.aria-label]="'common.delete' | t"
              (click)="deleteWorkout(x)"
            >
              <ng-icon name="lucideTrash2" class="text-muted-foreground" />
            </button>
          </tl-list-item>
        }
      </tl-list>
    } @else if (w) {
      <tl-card><p class="text-muted-foreground text-sm">{{ 'week.noWorkouts' | t }}</p></tl-card>
    }

    <app-workout-sheet [(open)]="workoutOpen" (saved)="week.reload()" />
  `,
})
export class WeekPage {
  protected readonly i18n = inject(I18n);
  private readonly api = inject(Api);

  protected readonly icons = WORKOUT_ICONS;
  protected readonly today = isoDate();

  protected readonly week = httpResource<WeekView>(() => '/api/week');
  protected readonly habitDay = httpResource<HabitDay[]>(() => `/api/habits/day?date=${this.today}`);
  protected readonly weekData = safeValue(this.week);
  protected readonly habitDayData = safeValue(this.habitDay);
  /** Local copy for optimistic toggling. */
  protected readonly habits = linkedSignal(() => this.habitDayData() ?? []);
  protected readonly workoutOpen = signal(false);

  protected async toggle(habit: HabitDay): Promise<void> {
    const done = !habit.done;
    this.habits.update((list) => list.map((h) => (h.id === habit.id ? { ...h, done } : h)));
    try {
      await this.api.setHabitDone(habit.id, this.today, done);
    } catch {
      this.habitDay.reload();
    }
    this.week.reload();
  }

  protected async deleteWorkout(workout: Workout): Promise<void> {
    if (confirm(this.i18n.t('week.deleteWorkoutConfirm'))) {
      await this.api.deleteWorkout(workout.id);
      this.week.reload();
    }
  }

  protected categoryKey(w: Workout): TranslationKey {
    return `workout.${w.category}`;
  }

  protected workoutSubtitle(w: Workout): string {
    const day = this.i18n.date(w.startedAt, { weekday: 'short', day: 'numeric', month: 'numeric' });
    return `${day} · ${w.durationMin} ${this.i18n.t('workout.minutes')}`;
  }
}
