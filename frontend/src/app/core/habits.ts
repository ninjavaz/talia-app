import { HabitKind } from './api';
import { I18n } from './i18n/i18n';

const NBSP = String.fromCharCode(0xa0);

/** How a habit is shown: the steps habit is named after the daily steps goal (REQ-013). */
export function habitLabel(
  habit: { name: string; kind: HabitKind },
  stepsGoal: number | null | undefined,
  i18n: I18n,
): string {
  if (habit.kind !== 'STEPS') {
    return habit.name;
  }
  return i18n.t('habit.steps', {
    goal: groupThousands(stepsGoal ?? 0, i18n.lang() === 'pl' ? NBSP : ','),
  });
}

/** "08:05" -> "8:05"; null stays null (REQ-011). */
export function habitTime(remindAt: string | null | undefined): string | null {
  if (!remindAt) {
    return null;
  }
  const [h, m] = remindAt.split(':');
  return `${Number(h)}:${m}`;
}

/** 8000 -> "8 000" / "8,000" (Intl does not group 4-digit numbers in Polish). */
function groupThousands(value: number, separator: string): string {
  return String(Math.round(value)).replace(/\B(?=(\d{3})+(?!\d))/g, separator);
}
