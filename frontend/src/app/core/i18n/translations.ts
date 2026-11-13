export type Lang = 'pl' | 'en';

export const LANGS: readonly Lang[] = ['pl', 'en'];

/** Polish is the source of truth: every key must exist here, English is type-checked against it. */
const pl = {
  'app.name': 'talia',

  'nav.progress': 'Postęp',
  'nav.week': 'Tydzień',
  'nav.summary': 'Podsumowanie',
  'nav.settings': 'Ustawienia',

  'common.noData': 'brak danych',
  'common.soon': 'wkrótce',

  'login.subtitle': 'Talia, waga, aktywność i nawyki',
  'login.username': 'Login',
  'login.password': 'Hasło',
  'login.submit': 'Zaloguj',
  'login.error': 'Nieprawidłowy login lub hasło',
  'login.networkError': 'Brak połączenia z serwerem',

  'progress.panel': 'Postęp',
  'progress.waist': 'Talia',
  'progress.weightTrend': 'Waga (trend)',
  'progress.chartEmpty': 'Wykres pojawi się po pierwszych pomiarach',
  'progress.today': 'Dziś',
  'progress.steps': 'Kroki',
  'progress.workouts': 'Treningi',
  'progress.habits': 'Nawyki',
  'progress.measurements': 'Pomiary',
  'progress.waistHint': 'Raz w tygodniu, najlepiej w niedzielę rano',
  'progress.weightHint': 'Automatycznie z Apple Health',
  'progress.addWaist': 'Dodaj pomiar talii',

  'week.title': 'Ten tydzień',
  'week.panel': 'Treningi',
  'week.of': 'z {goal}',
  'week.avgSteps': 'Kroki śr.',
  'week.habits': 'Nawyki',
  'week.workoutsList': 'Treningi w tym tygodniu',
  'week.noWorkouts': 'Brak treningów w tym tygodniu. Pojawią się same z zegarka.',
  'week.habitsToday': 'Nawyki dziś',
  'week.habitsEmpty': 'Lista nawyków pojawi się w kolejnym kroku.',

  'summary.title': 'Podsumowanie tygodnia',
  'summary.panel': 'Komentarz AI',
  'summary.soonTitle': 'Wkrótce',
  'summary.soonBody':
    'W kolejnej wersji co niedzielę pojawi się tu krótki komentarz: co się zmieniło w tym tygodniu i dlaczego.',
  'summary.numbers': 'Liczby tygodnia',

  'settings.title': 'Ustawienia',
  'settings.appearance': 'Wygląd',
  'settings.theme.system': 'Auto',
  'settings.theme.light': 'Jasny',
  'settings.theme.dark': 'Ciemny',
  'settings.language': 'Język',
  'settings.goals': 'Cele',
  'settings.habits': 'Nawyki',
  'settings.account': 'Konto',
  'settings.logout': 'Wyloguj',
  'settings.loggedAs': 'Zalogowano jako {user}',
} as const;

export type TranslationKey = keyof typeof pl;

const en: Record<TranslationKey, string> = {
  'app.name': 'talia',

  'nav.progress': 'Progress',
  'nav.week': 'Week',
  'nav.summary': 'Summary',
  'nav.settings': 'Settings',

  'common.noData': 'no data',
  'common.soon': 'coming soon',

  'login.subtitle': 'Waist, weight, activity and habits',
  'login.username': 'Username',
  'login.password': 'Password',
  'login.submit': 'Sign in',
  'login.error': 'Wrong username or password',
  'login.networkError': 'Cannot reach the server',

  'progress.panel': 'Progress',
  'progress.waist': 'Waist',
  'progress.weightTrend': 'Weight (trend)',
  'progress.chartEmpty': 'The chart will appear after the first measurements',
  'progress.today': 'Today',
  'progress.steps': 'Steps',
  'progress.workouts': 'Workouts',
  'progress.habits': 'Habits',
  'progress.measurements': 'Measurements',
  'progress.waistHint': 'Once a week, ideally Sunday morning',
  'progress.weightHint': 'Automatically from Apple Health',
  'progress.addWaist': 'Add waist measurement',

  'week.title': 'This week',
  'week.panel': 'Workouts',
  'week.of': 'of {goal}',
  'week.avgSteps': 'Avg. steps',
  'week.habits': 'Habits',
  'week.workoutsList': 'Workouts this week',
  'week.noWorkouts': 'No workouts this week yet. They will come from the watch automatically.',
  'week.habitsToday': 'Habits today',
  'week.habitsEmpty': 'The habit list is coming in the next step.',

  'summary.title': 'Weekly summary',
  'summary.panel': 'AI comment',
  'summary.soonTitle': 'Coming soon',
  'summary.soonBody':
    'In the next version a short comment will appear here every Sunday: what changed this week and why.',
  'summary.numbers': 'Numbers of the week',

  'settings.title': 'Settings',
  'settings.appearance': 'Appearance',
  'settings.theme.system': 'Auto',
  'settings.theme.light': 'Light',
  'settings.theme.dark': 'Dark',
  'settings.language': 'Language',
  'settings.goals': 'Goals',
  'settings.habits': 'Habits',
  'settings.account': 'Account',
  'settings.logout': 'Sign out',
  'settings.loggedAs': 'Signed in as {user}',
};

export const TRANSLATIONS: Record<Lang, Record<TranslationKey, string>> = { pl, en };

export const LOCALES: Record<Lang, string> = { pl: 'pl-PL', en: 'en-GB' };
