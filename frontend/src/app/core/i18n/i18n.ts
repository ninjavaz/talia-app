import { DOCUMENT, Injectable, Pipe, PipeTransform, computed, effect, inject, signal } from '@angular/core';
import { LANGS, LOCALES, Lang, TRANSLATIONS, TranslationKey } from './translations';

const STORAGE_KEY = 'talia.lang';

/**
 * Minimal runtime i18n: a signal holding the language, a lookup with {param} interpolation and
 * locale-aware number/date formatting. Switching the language re-renders instantly.
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

  /** 64.25 -> "64,3" (pl) / "64.3" (en). */
  num(value: number | null | undefined, maxFraction = 1, minFraction = 0): string {
    if (value === null || value === undefined) {
      return '—';
    }
    return new Intl.NumberFormat(this.locale(), {
      maximumFractionDigits: maxFraction,
      minimumFractionDigits: minFraction,
    }).format(value);
  }

  /** 8123 -> "8,1k" */
  compact(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '—';
    }
    return value >= 1000 ? `${this.num(value / 1000, 1)}k` : this.num(value, 0);
  }

  /** ISO date -> formatted with the given Intl options, e.g. { weekday: 'short', day: 'numeric', month: 'numeric' }. */
  date(iso: string, options: Intl.DateTimeFormatOptions): string {
    const [y, m, d] = iso.slice(0, 10).split('-').map(Number);
    return new Intl.DateTimeFormat(this.locale(), options).format(new Date(y, m - 1, d));
  }

  /** "9–15.11" for a Monday..Sunday range. */
  weekRange(startIso: string, endIso: string): string {
    const day = (iso: string) => Number(iso.slice(8, 10));
    const month = (iso: string) => iso.slice(5, 7);
    return month(startIso) === month(endIso)
      ? `${day(startIso)}–${day(endIso)}.${month(endIso)}`
      : `${day(startIso)}.${month(startIso)}–${day(endIso)}.${month(endIso)}`;
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
