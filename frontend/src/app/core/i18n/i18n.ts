import { DOCUMENT, Injectable, Pipe, PipeTransform, computed, effect, inject, signal } from '@angular/core';
import { LANGS, LOCALES, Lang, TRANSLATIONS, TranslationKey } from './translations';

const STORAGE_KEY = 'talia.lang';

/**
 * Minimal runtime i18n: a signal holding the language and a lookup with {param} interpolation.
 * Switching the language re-renders instantly, no reload and no per-language build.
 */
@Injectable({ providedIn: 'root' })
export class I18n {
  private readonly document = inject(DOCUMENT);

  readonly lang = signal<Lang>(readStoredLang());
  readonly locale = computed(() => LOCALES[this.lang()]);

  /** "piątek, 13.11" / "Friday 13/11" */
  readonly today = computed(() =>
    new Intl.DateTimeFormat(this.locale(), { weekday: 'long', day: '2-digit', month: '2-digit' }).format(
      new Date(),
    ),
  );

  constructor() {
    effect(() => {
      this.document.documentElement.lang = this.lang();
    });
  }

  setLang(lang: Lang): void {
    this.lang.set(lang);
    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch {
      // private mode etc. - language still applies for this session
    }
  }

  t(key: TranslationKey, params?: Record<string, string | number>): string {
    const text = TRANSLATIONS[this.lang()][key] ?? key;
    return params ? text.replace(/\{(\w+)\}/g, (m, name) => String(params[name] ?? m)) : text;
  }
}

/** `{{ 'nav.week' | t }}` - impure so it follows the language signal. */
@Pipe({ name: 't', pure: false })
export class TranslatePipe implements PipeTransform {
  private readonly i18n = inject(I18n);

  transform(key: TranslationKey, params?: Record<string, string | number>): string {
    return this.i18n.t(key, params);
  }
}

function readStoredLang(): Lang {
  try {
    const stored = localStorage.getItem(STORAGE_KEY) as Lang | null;
    if (stored && LANGS.includes(stored)) {
      return stored;
    }
  } catch {
    // ignore
  }
  return 'pl';
}
