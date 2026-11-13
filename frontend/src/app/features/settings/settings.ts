import { httpResource } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import {
  TlButton,
  TlCard,
  TlField,
  TlInput,
  TlList,
  TlListItem,
  TlPageHeader,
  TlRoundButton,
  TlSectionHeader,
  TlSegmentOption,
  TlSegmented,
  TlLogo,
  TlToast,
} from '@talia/ui';
import { Api, BodyPart, Habit, Settings, safeValue } from '../../core/api';
import { Auth } from '../../core/auth/auth';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';
import { Lang, TranslationKey } from '../../core/i18n/translations';
import { Theme } from '../../core/theme/theme';

@Component({
  selector: 'app-settings',
  imports: [
    FormsModule,
    NgIcon,
    TlButton,
    TlCard,
    TlField,
    TlInput,
    TlList,
    TlListItem,
    TlPageHeader,
    TlRoundButton,
    TlSectionHeader,
    TlSegmented,
    TlLogo,
    TranslatePipe,
  ],
  template: `
    <tl-page-header [title]="'settings.title' | t" />

    <tl-section-header [title]="'settings.goals' | t" />
    <tl-card>
      <form class="flex flex-col gap-4" (ngSubmit)="saveGoals()">
        <div class="grid grid-cols-2 gap-3">
          <tl-field [label]="'settings.workoutGoal' | t">
            <input tlInput name="workouts" inputmode="numeric" [(ngModel)]="workoutGoal" />
          </tl-field>
          <tl-field [label]="'settings.stepsGoal' | t">
            <input tlInput name="steps" inputmode="numeric" [(ngModel)]="stepsGoal" />
          </tl-field>
        </div>
        <tl-field [label]="'settings.threshold' | t" unit="%">
          <input tlInput name="threshold" inputmode="numeric" [(ngModel)]="threshold" />
        </tl-field>
        @if (goalsError()) {
          <p class="text-destructive text-sm" role="alert">{{ 'common.saveError' | t }}</p>
        }
        <button tlButton type="submit" [disabled]="busy()">
          {{ (goalsSaved() ? 'settings.saved' : 'common.save') | t }}
        </button>
      </form>
    </tl-card>

    <tl-section-header [title]="'settings.habits' | t" />
    <tl-list>
      @for (h of habitsData() ?? []; track h.id) {
        <tl-list-item icon="lucideListChecks" [title]="h.name">
          <span tlListTrailing class="flex gap-1">
            <button tlRoundButton size="sm" variant="ghost" [attr.aria-label]="'settings.renamePrompt' | t" (click)="rename(h)">
              <ng-icon name="lucidePencil" class="text-muted-foreground" />
            </button>
            <button tlRoundButton size="sm" variant="ghost" [attr.aria-label]="'common.delete' | t" (click)="removeHabit(h)">
              <ng-icon name="lucideTrash2" class="text-muted-foreground" />
            </button>
          </span>
        </tl-list-item>
      }
      <form class="flex items-center gap-2 py-3" (ngSubmit)="addHabit()">
        <input tlInput name="newHabit" maxlength="100" [placeholder]="'settings.newHabit' | t" [(ngModel)]="newHabit" />
        <button tlRoundButton variant="solid" type="submit" [attr.aria-label]="'common.add' | t" [disabled]="!newHabit.trim()">
          <ng-icon name="lucidePlus" />
        </button>
      </form>
    </tl-list>
    <p class="text-muted-foreground mt-2 px-2 text-xs">{{ 'settings.habitsHint' | t }}</p>

    <tl-section-header [title]="'settings.bodyParts' | t" />
    <tl-list>
      @for (b of bodyPartsData() ?? []; track b.id) {
        <tl-list-item icon="lucideRuler" [title]="b.code === 'WAIST' ? ('progress.waist' | t) : b.name">
          <span tlListTrailing class="flex gap-1">
            @if (b.code !== 'WAIST') {
              <button tlRoundButton size="sm" variant="ghost" [attr.aria-label]="'settings.renameBodyPart' | t" (click)="renameBodyPart(b)">
                <ng-icon name="lucidePencil" class="text-muted-foreground" />
              </button>
              <button tlRoundButton size="sm" variant="ghost" [attr.aria-label]="'common.delete' | t" (click)="removeBodyPart(b)">
                <ng-icon name="lucideTrash2" class="text-muted-foreground" />
              </button>
            }
          </span>
        </tl-list-item>
      }
      <form class="flex items-center gap-2 py-3" (ngSubmit)="addBodyPart()">
        <input tlInput name="newBodyPart" maxlength="50" [placeholder]="'settings.newBodyPart' | t" [(ngModel)]="newBodyPart" />
        <button tlRoundButton variant="solid" type="submit" [attr.aria-label]="'common.add' | t" [disabled]="!newBodyPart().trim()">
          <ng-icon name="lucidePlus" />
        </button>
      </form>
    </tl-list>
    <p class="text-muted-foreground mt-2 px-2 text-xs">{{ 'settings.bodyPartsHint' | t }}</p>

    <tl-section-header [title]="'settings.appearance' | t" />
    <tl-card>
      <tl-segmented [options]="themeOptions()" [value]="theme.mode()" (valueChange)="setTheme($event)" />
    </tl-card>

    <tl-section-header [title]="'settings.language' | t" />
    <tl-card>
      <tl-segmented [options]="langOptions" [value]="i18n.lang()" (valueChange)="setLang($event)" />
    </tl-card>

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

    <footer class="text-muted-foreground mt-10 flex flex-col items-center gap-2 text-center text-sm">
      <tl-logo [size]="40" wordmark class="text-foreground" />
      <p>{{ 'login.slogan' | t }}</p>
    </footer>
  `,
})
export class SettingsPage {
  protected readonly theme = inject(Theme);
  protected readonly i18n = inject(I18n);
  protected readonly auth = inject(Auth);
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  protected readonly settings = httpResource<Settings>(() => '/api/settings');
  protected readonly habits = httpResource<Habit[]>(() => '/api/habits');
  protected readonly settingsData = safeValue(this.settings);
  protected readonly habitsData = safeValue(this.habits);
  protected readonly bodyParts = httpResource<BodyPart[]>(() => '/api/body-parts');
  protected readonly bodyPartsData = safeValue(this.bodyParts);
  protected readonly newBodyPart = signal('');
  private readonly toast = inject(TlToast);

  protected readonly workoutGoal = signal('');
  protected readonly stepsGoal = signal('');
  protected readonly threshold = signal('');
  protected newHabit = '';
  protected readonly busy = signal(false);
  protected readonly goalsSaved = signal(false);
  protected readonly goalsError = signal(false);

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

  constructor() {
    effect(() => {
      const s = this.settingsData();
      if (s) {
        this.workoutGoal.set(String(s.weeklyWorkoutGoal));
        this.stepsGoal.set(String(s.dailyStepsGoal));
        this.threshold.set(String(s.habitWeekThresholdPct));
      }
    });
  }

  protected async saveGoals(): Promise<void> {
    const current = this.settingsData();
    if (!current) {
      return;
    }
    await this.persistSettings({
      ...current,
      weeklyWorkoutGoal: Number(this.workoutGoal()),
      dailyStepsGoal: Number(this.stepsGoal().replace(/\s/g, '')),
      habitWeekThresholdPct: Number(this.threshold()),
    });
  }

  protected async addHabit(): Promise<void> {
    const name = this.newHabit.trim();
    if (name && (await this.attempt(() => this.api.addHabit(name), 'common.added'))) {
      this.newHabit = '';
      this.habits.reload();
    }
  }

  protected async rename(habit: Habit): Promise<void> {
    const name = prompt(this.i18n.t('settings.renamePrompt'), habit.name)?.trim();
    if (name && name !== habit.name && (await this.attempt(() => this.api.renameHabit(habit.id, name), 'common.saved'))) {
      this.habits.reload();
    }
  }

  protected async removeHabit(habit: Habit): Promise<void> {
    if (
      confirm(this.i18n.t('settings.deleteHabitConfirm', { name: habit.name })) &&
      (await this.attempt(() => this.api.deleteHabit(habit.id), 'common.deleted'))
    ) {
      this.habits.reload();
    }
  }

  protected async addBodyPart(): Promise<void> {
    const name = this.newBodyPart().trim();
    if (name && (await this.attempt(() => this.api.addBodyPart(name), 'common.added'))) {
      this.newBodyPart.set('');
      this.bodyParts.reload();
    }
  }

  protected async renameBodyPart(part: BodyPart): Promise<void> {
    const name = prompt(this.i18n.t('settings.renameBodyPart'), part.name)?.trim();
    if (name && name !== part.name && (await this.attempt(() => this.api.renameBodyPart(part.id, name), 'common.saved'))) {
      this.bodyParts.reload();
    }
  }

  protected async removeBodyPart(part: BodyPart): Promise<void> {
    if (
      confirm(this.i18n.t('settings.deleteBodyPartConfirm', { name: part.name })) &&
      (await this.attempt(() => this.api.deleteBodyPart(part.id), 'common.deleted'))
    ) {
      this.bodyParts.reload();
    }
  }

  /** Runs a write, shows a green confirmation or a red error toast; true on success. */
  private async attempt(action: () => Promise<unknown>, successKey: TranslationKey): Promise<boolean> {
    try {
      await action();
      this.toast.success(this.i18n.t(successKey));
      return true;
    } catch {
      this.toast.error(this.i18n.t('common.saveError'));
      return false;
    }
  }

  protected setTheme(value: string | undefined): void {
    if (value === 'system' || value === 'light' || value === 'dark') {
      this.theme.setMode(value);
    }
  }

  protected setLang(value: string | undefined): void {
    if (value === 'pl' || value === 'en') {
      this.i18n.setLang(value as Lang);
      const current = this.settingsData();
      if (current && current.language !== value) {
        // keep the server in sync (used later for notification texts); failures are not critical
        void this.api.saveSettings({ ...current, language: value }).then((s) => this.settings.set(s), () => {});
      }
    }
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigate(['/login']);
  }

  private async persistSettings(body: Settings): Promise<void> {
    this.busy.set(true);
    this.goalsError.set(false);
    try {
      this.settings.set(await this.api.saveSettings(body));
      this.goalsSaved.set(true);
      this.toast.success(this.i18n.t('common.saved'));
      setTimeout(() => this.goalsSaved.set(false), 2000);
    } catch {
      this.goalsError.set(true);
      this.toast.error(this.i18n.t('common.saveError'));
    } finally {
      this.busy.set(false);
    }
  }
}
