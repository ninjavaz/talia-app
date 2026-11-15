import { httpResource } from '@angular/common/http';
import { Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import {
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
  TlStat,
  TlToast,
  diamondBurst,
} from '@talia/ui';
import { Api, HabitDay, WeekView, Workout, isoDate, safeValue } from '../../core/api';
import { habitLabel, habitTime } from '../../core/habits';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';
import { TranslationKey } from '../../core/i18n/translations';
import { WORKOUT_ICONS } from '../../core/icons';
import { WorkoutSheet } from './workout-sheet';

/** Start screen (REQ-052): today's habits with progress, then the week's workouts vs goal and the streak. */
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
    TlProgressBar,
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

    <!-- REQ-052: today's habits come first; every tap here is a complete entry -->
    <tl-section-header [title]="'week.habitsToday' | t">
      <span class="text-muted-foreground text-sm first-letter:uppercase">
        {{ i18n.date(today, { weekday: 'long', day: 'numeric', month: 'numeric' }) }}
      </span>
    </tl-section-header>
    @if (habits().length) {
      <tl-card class="mb-3" data-testid="today-progress">
        <div class="mb-2 flex items-baseline justify-between text-sm">
          <span class="font-semibold">
            {{ (doneToday() === habits().length ? 'week.allDone' : 'week.todayProgress') | t: { done: doneToday(), total: habits().length } }}
          </span>
          <span class="text-muted-foreground tabular-nums">{{ doneToday() }}/{{ habits().length }}</span>
        </div>
        <tl-progress-bar [value]="doneToday()" [max]="habits().length" />
      </tl-card>
      <tl-list>
        @for (h of habits(); track h.id) {
          <tl-list-item [title]="label(h)" [subtitle]="time(h.remindAt)">
            <button tlListTrailing tlCheck [checked]="h.done" [attr.aria-label]="label(h)" (click)="toggle(h, $event)"></button>
          </tl-list-item>
        }
      </tl-list>
    } @else if (!habitDay.isLoading()) {
      <a routerLink="/settings">
        <tl-card><p class="text-muted-foreground text-sm">{{ 'week.habitsEmpty' | t }}</p></tl-card>
      </a>
    }

    <tl-panel class="mt-6" [title]="'week.panel' | t">
      <div class="flex items-center justify-between">
        <tl-stat class="w-20" [value]="i18n.compact(w?.avgDailySteps)" [label]="'week.avgSteps' | t" />
        <tl-ring [value]="w?.workoutsDone ?? 0" [max]="w?.workoutGoal || 1" [size]="150" [stroke]="12">
          <span class="text-4xl font-semibold tabular-nums">{{ w?.workoutsDone ?? 0 }}</span>
          <span class="text-muted-foreground text-xs">{{ 'week.of' | t: { goal: w?.workoutGoal ?? 0 } }}</span>
        </tl-ring>
        <tl-stat class="w-20" data-testid="streak" [value]="w?.habits?.streakDays ?? 0" [label]="'week.streak' | t" />
      </div>
    </tl-panel>

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
  private readonly toast = inject(TlToast);

  protected readonly icons = WORKOUT_ICONS;
  protected readonly today = isoDate();

  protected readonly week = httpResource<WeekView>(() => '/api/week');
  protected readonly habitDay = httpResource<HabitDay[]>(() => `/api/habits/day?date=${this.today}`);
  protected readonly weekData = safeValue(this.week);
  protected readonly habitDayData = safeValue(this.habitDay);
  /** Local copy for optimistic toggling. */
  protected readonly habits = linkedSignal(() => this.habitDayData() ?? []);
  protected readonly workoutOpen = signal(false);
  /** Counted from the local list, so the bar moves on the tap, before the server answers (REQ-014). */
  protected readonly doneToday = computed(() => this.habits().filter((h) => h.done).length);

  protected label(h: HabitDay): string {
    return habitLabel(h, this.weekData()?.dailyStepsGoal, this.i18n);
  }

  protected time(remindAt: string | null): string | null {
    return habitTime(remindAt);
  }

  protected async toggle(habit: HabitDay, event: Event): Promise<void> {
    const done = !habit.done;
    this.habits.update((list) => list.map((h) => (h.id === habit.id ? { ...h, done } : h)));
    if (done) {
      const allDone = this.habits().every((h) => h.done);
      diamondBurst(event.currentTarget as Element, allDone ? { count: 40, spread: 220, height: 280 } : {});
    }
    try {
      await this.api.setHabitDone(habit.id, this.today, done);
    } catch {
      this.habitDay.reload();
    }
    this.week.reload();
  }

  protected async deleteWorkout(workout: Workout): Promise<void> {
    if (confirm(this.i18n.t('week.deleteWorkoutConfirm'))) {
      try {
        await this.api.deleteWorkout(workout.id);
        this.toast.success(this.i18n.t('common.deleted'));
      } catch {
        this.toast.error(this.i18n.t('common.saveError'));
      }
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
