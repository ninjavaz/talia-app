export type Lang = 'pl' | 'en';

export const LANGS: readonly Lang[] = ['pl', 'en'];

/** Polish is the source of truth: every key must exist here, English is type-checked against it. */
const pl = {
  'app.name': 'Talia',

  'nav.progress': 'Postęp',
  'nav.week': 'Tydzień',
  'nav.summary': 'Podsumowanie',
  'nav.settings': 'Ustawienia',

  'login.subtitle': 'Talia, waga, aktywność i nawyki',
  'login.username': 'Login',
  'login.password': 'Hasło',
  'login.submit': 'Zaloguj',
  'login.error': 'Nieprawidłowy login lub hasło',
  'login.networkError': 'Brak połączenia z serwerem',

  'progress.title': 'Postęp',
  'progress.waist': 'Talia',
  'progress.weightTrend': 'Waga (trend)',
  'progress.fromStart': 'od startu',
  'progress.noData': 'Brak danych',
  'progress.chartEmpty': 'Wykres pojawi się po pierwszych pomiarach',
  'progress.legendPoints': 'pomiary',
  'progress.legendTrend': 'trend 7 dni',
  'progress.addWaist': 'Dodaj pomiar talii',
  'progress.stepsToday': 'Kroki dziś',

  'week.title': 'Ten tydzień',
  'week.workouts': 'Treningi: {done} z {goal}',
  'week.noWorkouts': 'Brak treningów w tym tygodniu',
  'week.habitsToday': 'Nawyki dziś',
  'week.habitsEmpty': 'Nawyki pojawią się wkrótce',
  'week.streak': 'Seria: {weeks} tyg. z rzędu',

  'summary.title': 'Podsumowanie tygodnia',
  'summary.soonTitle': 'Wkrótce',
  'summary.soonBody':
    'W kolejnej wersji co niedzielę pojawi się tu krótki komentarz AI: co się zmieniło w tym tygodniu i dlaczego.',

  'settings.title': 'Ustawienia',
  'settings.appearance': 'Wygląd',
  'settings.theme.system': 'Systemowy',
  'settings.theme.light': 'Jasny',
  'settings.theme.dark': 'Ciemny',
  'settings.language': 'Język',
  'settings.goals': 'Cele i nawyki',
  'settings.goalsSoon': 'Konfiguracja celów i nawyków pojawi się w kolejnym kroku.',
  'settings.account': 'Konto',
  'settings.loggedAs': 'Zalogowano jako {user}',
  'settings.logout': 'Wyloguj',
} as const;

export type TranslationKey = keyof typeof pl;

const en: Record<TranslationKey, string> = {
  'app.name': 'Talia',

  'nav.progress': 'Progress',
  'nav.week': 'Week',
  'nav.summary': 'Summary',
  'nav.settings': 'Settings',

  'login.subtitle': 'Waist, weight, activity and habits',
  'login.username': 'Username',
  'login.password': 'Password',
  'login.submit': 'Sign in',
  'login.error': 'Wrong username or password',
  'login.networkError': 'Cannot reach the server',

  'progress.title': 'Progress',
  'progress.waist': 'Waist',
  'progress.weightTrend': 'Weight (trend)',
  'progress.fromStart': 'since start',
  'progress.noData': 'No data',
  'progress.chartEmpty': 'The chart will appear after the first measurements',
  'progress.legendPoints': 'measurements',
  'progress.legendTrend': '7-day trend',
  'progress.addWaist': 'Add waist measurement',
  'progress.stepsToday': 'Steps today',

  'week.title': 'This week',
  'week.workouts': 'Workouts: {done} of {goal}',
  'week.noWorkouts': 'No workouts this week',
  'week.habitsToday': 'Habits today',
  'week.habitsEmpty': 'Habits are coming soon',
  'week.streak': 'Streak: {weeks} weeks in a row',

  'summary.title': 'Weekly summary',
  'summary.soonTitle': 'Coming soon',
  'summary.soonBody':
    'In the next version a short AI comment will appear here every Sunday: what changed this week and why.',

  'settings.title': 'Settings',
  'settings.appearance': 'Appearance',
  'settings.theme.system': 'System',
  'settings.theme.light': 'Light',
  'settings.theme.dark': 'Dark',
  'settings.language': 'Language',
  'settings.goals': 'Goals and habits',
  'settings.goalsSoon': 'Goal and habit configuration is coming in the next step.',
  'settings.account': 'Account',
  'settings.loggedAs': 'Signed in as {user}',
  'settings.logout': 'Sign out',
};

export const TRANSLATIONS: Record<Lang, Record<TranslationKey, string>> = { pl, en };
