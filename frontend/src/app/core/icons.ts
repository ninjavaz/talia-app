import { provideIcons } from '@ng-icons/core';
import {
  lucideCalendarCheck,
  lucideChevronRight,
  lucideDumbbell,
  lucideFootprints,
  lucideGoal,
  lucideLanguages,
  lucideListChecks,
  lucideLogOut,
  lucidePlus,
  lucideRuler,
  lucideScale,
  lucideSettings,
  lucideSparkles,
  lucideTrendingDown,
} from '@ng-icons/lucide';

/** All icons used by name in templates (e.g. `icon="lucideRuler"`), registered once app-wide. */
export const provideAppIcons = () =>
  provideIcons({
    lucideCalendarCheck,
    lucideChevronRight,
    lucideDumbbell,
    lucideFootprints,
    lucideGoal,
    lucideLanguages,
    lucideListChecks,
    lucideLogOut,
    lucidePlus,
    lucideRuler,
    lucideScale,
    lucideSettings,
    lucideSparkles,
    lucideTrendingDown,
  });
