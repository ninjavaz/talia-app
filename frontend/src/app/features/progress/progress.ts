import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import {
  TlButton,
  TlCard,
  TlList,
  TlListItem,
  TlPageHeader,
  TlPanel,
  TlRing,
  TlRoundButton,
  TlSectionHeader,
  TlSegmented,
  TlStat,
  TlStatTone,
  TlTrendChart,
} from '@talia/ui';
import { Metric, Progress, WeekEntry, WeekView, addDays, isoDate, safeValue } from '../../core/api';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';
import { CheckinPart, CheckinSheet } from './checkin-sheet';

/** 'weight' or 'part-<id>' */
type ChartMetric = string;

/** Waist + weight trend, this week at a glance, weekly check-in history and the Sunday reminder. */
@Component({
  selector: 'app-progress',
  imports: [
    CheckinSheet,
    NgIcon,
    RouterLink,
    TlButton,
    TlCard,
    TlList,
    TlListItem,
    TlPageHeader,
    TlPanel,
    TlRing,
    TlRoundButton,
    TlSectionHeader,
    TlSegmented,
    TlStat,
    TlTrendChart,
    TranslatePipe,
  ],
  template: `
    <tl-page-header [title]="i18n.today()">
      <button tlRoundButton [attr.aria-label]="'progress.addCheckin' | t" (click)="openCurrentWeek()">
        <ng-icon name="lucidePlus" />
      </button>
    </tl-page-header>

    @if (progress.error()) {
      <tl-card class="mb-4"><p class="text-destructive text-sm">{{ 'common.loadError' | t }}</p></tl-card>
    }

    @if (progressData()?.checkinDue) {
      <tl-card class="bg-progress/15 dark:bg-progress/10 mb-4 shadow-none">
        <div class="flex gap-3">
          <span class="bg-progress flex size-11 shrink-0 items-center justify-center rounded-2xl text-xl text-white">
            <ng-icon name="lucideBell" />
          </span>
          <div class="min-w-0">
            <p class="font-semibold">{{ 'progress.reminderTitle' | t }}</p>
            <p class="text-muted-foreground mt-0.5 text-sm">{{ 'progress.reminderBody' | t }}</p>
          </div>
        </div>
        <button tlButton class="mt-4 w-full" (click)="openCurrentWeek()">{{ 'progress.reminderAction' | t }}</button>
      </tl-card>
    }

    <tl-panel [title]="'progress.panel' | t">
      @let p = progressData();
      <div class="grid grid-cols-2">
        <!-- headline: the first body part on the user's list (none until they add one in Settings) -->
        @let first = p?.bodyParts?.[0];
        <tl-stat
          [value]="i18n.num(first?.metric?.latest)"
          [unit]="first?.metric ? 'cm' : null"
          [label]="first?.name ?? ('progress.measurements' | t)"
          [hint]="p && !first ? ('progress.addParts' | t) : deltaText(first?.metric, 'cm')"
          [tone]="tone(first?.metric)"
        />
        <tl-stat
          [value]="i18n.num(p?.weight?.latest)"
          [unit]="p?.weight ? 'kg' : null"
          [label]="'progress.weightTrend' | t"
          [hint]="deltaText(p?.weight, 'kg')"
          [tone]="tone(p?.weight)"
        />
      </div>

      <tl-segmented
        class="mt-5"
        [options]="chartOptions()"
        [value]="chartMetric()"
        (valueChange)="chartMetric.set($any($event))"
      />
      <tl-trend-chart
        class="mt-4"
        [points]="chartPoints()"
        [trend]="chartTrend()"
        [format]="formatAxis"
        [formatDate]="formatDate"
      >
        {{ 'progress.chartEmpty' | t }}
      </tl-trend-chart>
    </tl-panel>

    <tl-section-header [title]="'progress.thisWeek' | t" link="/week" />
    <a routerLink="/week" class="block transition duration-200 hover:scale-[1.01]">
      <tl-card>
        @let w = weekData();
        <div class="flex justify-around">
          <div class="flex flex-col items-center gap-2">
            <tl-ring [value]="stepsThisWeek() ?? 0" [max]="p?.dailyStepsGoal ?? 8000" [size]="76">
              <span class="font-semibold">{{ i18n.compact(stepsThisWeek()) }}</span>
            </tl-ring>
            <span class="text-muted-foreground text-xs">{{ 'progress.steps' | t }}</span>
          </div>
          <div class="flex flex-col items-center gap-2">
            <tl-ring [value]="w?.workoutsDone ?? 0" [max]="w?.workoutGoal ?? 3" [size]="76">
              <span class="font-semibold">{{ w?.workoutsDone ?? 0 }}/{{ w?.workoutGoal ?? 3 }}</span>
            </tl-ring>
            <span class="text-muted-foreground text-xs">{{ 'progress.workouts' | t }}</span>
          </div>
          <div class="flex flex-col items-center gap-2">
            <tl-ring [value]="w?.habits?.doneToday ?? 0" [max]="w?.habits?.total || 1" [size]="76">
              <span class="font-semibold">{{ w?.habits?.doneToday ?? 0 }}/{{ w?.habits?.total ?? 0 }}</span>
            </tl-ring>
            <span class="text-muted-foreground text-xs">{{ 'progress.habits' | t }}</span>
          </div>
        </div>
      </tl-card>
    </a>

    <tl-section-header [title]="'progress.history' | t" />
    @if (historyData()?.length) {
      <tl-list>
        @for (e of historyData(); track e.weekStart) {
          <button
            tlListItem
            icon="lucideRuler"
            [title]="i18n.weekRange(e.weekStart, addDays(e.weekStart, 6))"
            [subtitle]="summary(e)"
            (click)="openCheckin(e)"
          >
            <ng-icon tlListTrailing name="lucideChevronRight" class="text-muted-foreground text-xl" />
          </button>
        }
      </tl-list>
    } @else if (!history.isLoading()) {
      <tl-card><p class="text-muted-foreground text-sm">{{ 'progress.historyEmpty' | t }}</p></tl-card>
    }

    <app-checkin-sheet [(open)]="sheetOpen" [parts]="checkinParts()" [entry]="editing()" (saved)="reload()" />
  `,
})
export class ProgressPage {
  protected readonly i18n = inject(I18n);
  protected readonly addDays = addDays;

  protected readonly progress = httpResource<Progress>(() => '/api/progress');
  protected readonly week = httpResource<WeekView>(() => '/api/week');
  protected readonly history = httpResource<WeekEntry[]>(() => '/api/checkins');
  protected readonly progressData = safeValue(this.progress);
  protected readonly weekData = safeValue(this.week);
  protected readonly historyData = safeValue(this.history);

  protected readonly sheetOpen = signal(false);
  protected readonly editing = signal<WeekEntry | null>(null);
  protected readonly chartMetric = signal<ChartMetric>('weight');

  /** Active body parts of the user, for the check-in form and the chart switch. */
  protected readonly checkinParts = computed<CheckinPart[]>(() =>
    (this.progressData()?.bodyParts ?? []).map((p) => ({ id: p.id, label: p.name })),
  );

  protected readonly chartOptions = computed(() => [
    { value: 'weight', label: this.i18n.t('progress.weight') },
    ...this.checkinParts().map((p) => ({ value: `part-${p.id}`, label: p.label })),
  ]);

  private readonly chartPart = computed(() =>
    this.progressData()?.bodyParts.find((p) => `part-${p.id}` === this.chartMetric()),
  );

  protected readonly chartPoints = computed(() =>
    this.chartMetric() === 'weight' ? (this.progressData()?.weightPoints ?? []) : (this.chartPart()?.points ?? []),
  );

  protected readonly chartTrend = computed(() =>
    this.chartMetric() === 'weight' ? (this.progressData()?.weightTrend ?? []) : (this.chartPart()?.points ?? []),
  );

  protected readonly stepsThisWeek = computed(() => {
    const p = this.progressData();
    return p?.avgDailyStepsThisWeek ?? p?.avgDailyStepsLastWeek ?? null;
  });

  protected readonly formatAxis = (v: number) => this.i18n.num(v, 1);
  protected readonly formatDate = (iso: string) => this.i18n.date(iso, { day: 'numeric', month: 'numeric' });

  /** "+" edits this week's check-in when there already is one, otherwise starts a new one. */
  protected openCurrentWeek(): void {
    const today = isoDate();
    const current = this.historyData()?.find((e) => e.weekStart <= today && today <= addDays(e.weekStart, 6));
    this.openCheckin(current ?? null);
  }

  protected openCheckin(entry: WeekEntry | null): void {
    this.editing.set(entry);
    this.sheetOpen.set(true);
  }

  protected reload(): void {
    this.progress.reload();
    this.history.reload();
    this.week.reload();
  }

  protected deltaText(metric: Metric | null | undefined, unit: string): string {
    if (!metric) {
      return this.i18n.t('common.noData');
    }
    const arrow = metric.delta < 0 ? '▼' : metric.delta > 0 ? '▲' : '•';
    return this.i18n.t('progress.fromStart', { arrow, value: `${this.i18n.num(Math.abs(metric.delta), 1)} ${unit}` });
  }

  protected tone(metric: Metric | null | undefined): TlStatTone {
    if (!metric || metric.delta === 0) {
      return 'muted';
    }
    return metric.delta < 0 ? 'success' : 'danger';
  }

  protected summary(e: WeekEntry): string {
    const parts: string[] = [];
    const labels = new Map(this.checkinParts().map((p) => [p.id, p.label]));
    for (const m of e.measurements) {
      parts.push(`${labels.get(m.partId) ?? ''} ${this.i18n.num(m.valueCm, 1)}`.trim());
    }
    if (e.weightKg != null) parts.push(`${this.i18n.num(e.weightKg, 1)} kg`);
    if (e.avgDailySteps != null) parts.push(`${this.i18n.num(e.avgDailySteps, 0)} ${this.i18n.t('progress.stepsUnit')}`);
    return parts.join(' · ');
  }
}
