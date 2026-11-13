import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import {
  TlCard,
  TlList,
  TlListItem,
  TlPageHeader,
  TlSectionHeader,
  TlSegmentOption,
  TlSegmented,
} from '@talia/ui';
import { Auth } from '../../core/auth/auth';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';
import { Theme } from '../../core/theme/theme';

@Component({
  selector: 'app-settings',
  imports: [NgIcon, TlCard, TlList, TlListItem, TlPageHeader, TlSectionHeader, TlSegmented, TranslatePipe],
  template: `
    <tl-page-header [title]="'settings.title' | t" />

    <tl-section-header [title]="'settings.appearance' | t" />
    <tl-card>
      <tl-segmented [options]="themeOptions()" [value]="theme.mode()" (valueChange)="setTheme($event)" />
    </tl-card>

    <tl-section-header [title]="'settings.language' | t" />
    <tl-card>
      <tl-segmented [options]="langOptions" [value]="i18n.lang()" (valueChange)="setLang($event)" />
    </tl-card>

    <tl-section-header [title]="'settings.goals' | t" />
    <tl-list>
      <button tlListItem disabled icon="lucideGoal" [title]="'settings.goals' | t" [subtitle]="'common.soon' | t">
        <ng-icon tlListTrailing name="lucideChevronRight" class="text-muted-foreground text-xl" />
      </button>
      <button tlListItem disabled icon="lucideListChecks" [title]="'settings.habits' | t" [subtitle]="'common.soon' | t">
        <ng-icon tlListTrailing name="lucideChevronRight" class="text-muted-foreground text-xl" />
      </button>
    </tl-list>

    <tl-section-header [title]="'settings.account' | t" />
    <tl-list>
      <button
        tlListItem
        icon="lucideLogOut"
        [title]="'settings.logout' | t"
        [subtitle]="'settings.loggedAs' | t: { user: auth.user() ?? '' }"
        (click)="logout()"
      ></button>
    </tl-list>
  `,
})
export class SettingsPage {
  protected readonly theme = inject(Theme);
  protected readonly i18n = inject(I18n);
  protected readonly auth = inject(Auth);
  private readonly router = inject(Router);

  protected readonly themeOptions = computed<TlSegmentOption[]>(() => {
    this.i18n.lang();
    return [
      { value: 'system', label: this.i18n.t('settings.theme.system') },
      { value: 'light', label: this.i18n.t('settings.theme.light') },
      { value: 'dark', label: this.i18n.t('settings.theme.dark') },
    ];
  });

  protected readonly langOptions: TlSegmentOption[] = [
    { value: 'pl', label: 'Polski' },
    { value: 'en', label: 'English' },
  ];

  protected setTheme(value: string | undefined): void {
    if (value === 'system' || value === 'light' || value === 'dark') {
      this.theme.setMode(value);
    }
  }

  protected setLang(value: string | undefined): void {
    if (value === 'pl' || value === 'en') {
      this.i18n.setLang(value);
    }
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigate(['/login']);
  }
}
