import { provideIcons } from '@ng-icons/core';
import {
  lucideActivity,
  lucideBell,
  lucideCalendarCheck,
  lucideChevronRight,
  lucideDumbbell,
  lucideFootprints,
  lucideGoal,
  lucideLanguages,
  lucideListChecks,
  lucideLogOut,
  lucidePencil,
  lucidePersonStanding,
  lucidePlus,
  lucideRuler,
  lucideScale,
  lucideSettings,
  lucideSparkles,
  lucideTrash2,
  lucideTrendingDown,
} from '@ng-icons/lucide';
import { WorkoutCategory } from './api';

/** All icons used by name in templates (e.g. `icon="lucideRuler"`), registered once app-wide. */
export const provideAppIcons = () =>
  provideIcons({
    lucideActivity,
    lucideBell,
    lucideCalendarCheck,
    lucideChevronRight,
    lucideDumbbell,
    lucideFootprints,
    lucideGoal,
    lucideLanguages,
    lucideListChecks,
    lucideLogOut,
    lucidePencil,
    lucidePersonStanding,
    lucidePlus,
    lucideRuler,
    lucideScale,
    lucideSettings,
    lucideSparkles,
    lucideTrash2,
    lucideTrendingDown,
  });

export const WORKOUT_ICONS: Record<WorkoutCategory, string> = {
  GYM: 'lucideDumbbell',
  PILATES: 'lucidePersonStanding',
  WALK: 'lucideFootprints',
  OTHER: 'lucideActivity',
};
