import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, computed, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface Point {
  date: string;
  value: number;
}

export interface Metric {
  latest: number;
  latestDate: string;
  first: number;
  delta: number;
}

export interface Progress {
  waist: Metric | null;
  weight: Metric | null;
  waistPoints: Point[];
  weightPoints: Point[];
  weightTrend: Point[];
  avgDailyStepsThisWeek: number | null;
  avgDailyStepsLastWeek: number | null;
  dailyStepsGoal: number;
  lastCheckin: string | null;
  checkinDoneThisWeek: boolean;
  checkinDue: boolean;
}

export interface WeekEntry {
  weekStart: string;
  waistCm: number | null;
  weightKg: number | null;
  avgDailySteps: number | null;
}

export interface CheckinRequest {
  date: string;
  waistCm: number | null;
  weightKg: number | null;
  avgDailySteps: number | null;
}

export type WorkoutCategory = 'GYM' | 'PILATES' | 'WALK' | 'OTHER';

export interface Workout {
  id: number;
  startedAt: string;
  durationMin: number;
  type: string;
  category: WorkoutCategory;
  kcal: number | null;
}

export interface HabitStats {
  doneToday: number;
  total: number;
  weekPct: number;
  thresholdPct: number;
  streakWeeks: number;
}

export interface WeekView {
  weekStart: string;
  weekEnd: string;
  workoutGoal: number;
  workoutsDone: number;
  workouts: Workout[];
  avgDailySteps: number | null;
  dailyStepsGoal: number;
  habits: HabitStats;
}

export interface Habit {
  id: number;
  name: string;
  sortOrder: number;
}

export interface HabitDay {
  id: number;
  name: string;
  done: boolean;
}

export interface Settings {
  weeklyWorkoutGoal: number;
  dailyStepsGoal: number;
  habitWeekThresholdPct: number;
  language: 'pl' | 'en';
}

/** Write operations. Reads use `httpResource` in the pages so they reload declaratively. */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  saveCheckin(body: CheckinRequest): Promise<WeekEntry> {
    return firstValueFrom(this.http.put<WeekEntry>('/api/checkins', body));
  }

  deleteCheckin(weekStart: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/checkins/${weekStart}`));
  }

  addWorkout(body: { date: string; category: WorkoutCategory; durationMin: number }): Promise<Workout> {
    return firstValueFrom(this.http.post<Workout>('/api/workouts', body));
  }

  deleteWorkout(id: number): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/workouts/${id}`));
  }

  setHabitDone(id: number, date: string, done: boolean): Promise<unknown> {
    const url = `/api/habits/${id}/checks/${date}`;
    return firstValueFrom(done ? this.http.put(url, null) : this.http.delete(url));
  }

  addHabit(name: string): Promise<Habit> {
    return firstValueFrom(this.http.post<Habit>('/api/habits', { name }));
  }

  renameHabit(id: number, name: string): Promise<Habit> {
    return firstValueFrom(this.http.put<Habit>(`/api/habits/${id}`, { name }));
  }

  deleteHabit(id: number): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/habits/${id}`));
  }

  saveSettings(body: Settings): Promise<Settings> {
    return firstValueFrom(this.http.put<Settings>('/api/settings', body));
  }
}

/** Local date as ISO yyyy-MM-dd (not UTC - toISOString would shift the day around midnight). */
export function isoDate(date = new Date()): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  return isoDate(new Date(y, m - 1, d + days));
}

/** Accepts "64,2" as well as "64.2"; empty -> null. */
export function parseDecimal(raw: string | null | undefined): number | null {
  const text = (raw ?? '').trim().replace(',', '.').replace(/\s/g, '');
  if (!text) {
    return null;
  }
  const value = Number(text);
  return Number.isFinite(value) ? value : NaN;
}

/**
 * Angular's resource.value() throws while the resource is in an error state; templates read this instead,
 * which is simply undefined until data is available (and stays on the last good value on reload).
 */
export function safeValue<T>(resource: { hasValue(): boolean; value(): T | undefined }): Signal<T | undefined> {
  return computed(() => (resource.hasValue() ? resource.value() : undefined));
}
