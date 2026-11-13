import { TestBed } from '@angular/core/testing';
import { I18n } from './i18n';
import { TRANSLATIONS } from './translations';

describe('I18n', () => {
  beforeEach(() => localStorage.clear());

  it('defaults to Polish', () => {
    const i18n = TestBed.inject(I18n);
    expect(i18n.lang()).toBe('pl');
    expect(i18n.t('nav.week')).toBe('Tydzień');
  });

  it('switches language and interpolates params', () => {
    const i18n = TestBed.inject(I18n);
    i18n.setLang('en');
    expect(i18n.t('week.workouts', { done: 2, goal: 3 })).toBe('Workouts: 2 of 3');
    expect(localStorage.getItem('talia.lang')).toBe('en');
  });

  it('has no empty translations', () => {
    for (const dict of Object.values(TRANSLATIONS)) {
      for (const value of Object.values(dict)) {
        expect(value.trim()).not.toBe('');
      }
    }
  });
});
