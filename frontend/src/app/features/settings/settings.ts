import { httpResource } from '@angular/common/http';
import {
  CdkDrag,
  CdkDragDrop,
  CdkDragHandle,
  CdkDropList,
  moveItemInArray,
} from '@angular/cdk/drag-drop';
import { Component, computed, effect, inject, linkedSignal, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import {
  TlButton,
  TlCard,
  TlCheck,
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
import { Api, BodyPart, Habit, HabitKind, Settings, safeValue } from '../../core/api';
import { habitLabel, habitTime } from '../../core/habits';
import { Auth } from '../../core/auth/auth';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';
import { Lang, TranslationKey } from '../../core/i18n/translations';
import { Theme } from '../../core/theme/theme';
import { PushNotifications } from '../../core/push';
import { HabitSheet } from './habit-sheet';

@Component({
  selector: 'app-settings',
  imports: [
    CdkDrag,
    CdkDragHandle,
    CdkDropList,
    FormsModule,
    HabitSheet,
    NgIcon,
    TlButton,
    TlCard,
    TlCheck,
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
        @if (goalsError()) {
          <p class="text-destructive text-sm" role="alert">{{ 'common.saveError' | t }}</p>
        }
        <button tlButton type="submit" [disabled]="busy()">
          {{ (goalsSaved() ? 'settings.saved' : 'common.save') | t }}
        </button>
      </form>
    </tl-card>

    <tl-section-header [title]="'settings.habits' | t">
      <button
        tlRoundButton
        size="sm"
        [attr.aria-label]="'settings.addHabit' | t"
        (click)="openHabit(null, 'NORMAL')"
      >
        <ng-icon name="lucidePlus" />
      </button>
    </tl-section-header>
    <!-- REQ-010: drag by the handle to change the order (same order on the Week screen) -->
    <tl-list cdkDropList cdkDropListLockAxis="y" (cdkDropListDropped)="dropHabit($event)">
      @for (h of habitOrder(); track h.id) {
        <tl-list-item
          cdkDrag
          [icon]="h.kind === 'STEPS' ? 'lucideFootprints' : 'lucideListChecks'"
          [title]="habitName(h)"
          [subtitle]="time(h.remindAt)"
        >
          <span tlListTrailing class="flex items-center gap-1">
            <button
              tlRoundButton
              size="sm"
              variant="ghost"
              [attr.aria-label]="'habit.editTitle' | t"
              (click)="openHabit(h, h.kind)"
            >
              <ng-icon name="lucidePencil" class="text-muted-foreground" />
            </button>
            <button
              tlRoundButton
              size="sm"
              variant="ghost"
              [attr.aria-label]="'common.delete' | t"
              (click)="removeHabit(h)"
            >
              <ng-icon name="lucideTrash2" class="text-muted-foreground" />
            </button>
            <span
              cdkDragHandle
              class="text-muted-foreground flex size-9 cursor-grab touch-none items-center justify-center"
              [attr.aria-label]="'settings.dragHabit' | t"
            >
              <ng-icon name="lucideGripVertical" />
            </span>
          </span>
        </tl-list-item>
      }
    </tl-list>
    @if (habitsData() && !hasStepsHabit()) {
      <button tlButton variant="secondary" class="mt-3 w-full" (click)="openHabit(null, 'STEPS')">
        {{ 'settings.addStepsHabit' | t }}
      </button>
    }
    <p class="text-muted-foreground mt-2 px-2 text-xs">{{ 'settings.habitsHint' | t }}</p>

    <tl-section-header [title]="'settings.bodyParts' | t" />
    <tl-list>
      @for (b of bodyPartsData() ?? []; track b.id) {
        <tl-list-item icon="lucideRuler" [title]="b.name">
          <span tlListTrailing class="flex gap-1">
            <button
              tlRoundButton
              size="sm"
              variant="ghost"
              [attr.aria-label]="'settings.renameBodyPart' | t"
              (click)="renameBodyPart(b)"
            >
              <ng-icon name="lucidePencil" class="text-muted-foreground" />
            </button>
            <button
              tlRoundButton
              size="sm"
              variant="ghost"
              [attr.aria-label]="'common.delete' | t"
              (click)="removeBodyPart(b)"
            >
              <ng-icon name="lucideTrash2" class="text-muted-foreground" />
            </button>
          </span>
        </tl-list-item>
      }
      <form class="flex items-center gap-2 py-3" (ngSubmit)="addBodyPart()">
        <input
          tlInput
          name="newBodyPart"
          maxlength="50"
          [placeholder]="'settings.newBodyPart' | t"
          [(ngModel)]="newBodyPart"
        />
        <button
          tlRoundButton
          variant="solid"
          type="submit"
          [attr.aria-label]="'common.add' | t"
          [disabled]="!newBodyPart().trim()"
        >
          <ng-icon name="lucidePlus" />
        </button>
      </form>
    </tl-list>
    <p class="text-muted-foreground mt-2 px-2 text-xs">{{ 'settings.bodyPartsHint' | t }}</p>

    <!-- REQ-033: push on this device and the reminder switches -->
    <tl-section-header [title]="'push.title' | t" />
    <tl-card data-testid="push-device">
      @if (!push.supported) {
        <p class="text-muted-foreground text-sm">{{ 'push.unsupported' | t }}</p>
      } @else if (push.permission() === 'denied') {
        <p class="text-muted-foreground text-sm">{{ 'push.denied' | t }}</p>
      } @else if (push.subscribed()) {
        <p class="text-sm font-semibold">{{ 'push.enabledHere' | t }}</p>
        <div class="mt-3 grid grid-cols-2 gap-2">
          <button tlButton variant="secondary" [disabled]="pushBusy()" (click)="testPush()">
            {{ 'push.test' | t }}
          </button>
          <button tlButton variant="secondary" [disabled]="pushBusy()" (click)="disablePush()">
            {{ 'push.disable' | t }}
          </button>
        </div>
      } @else {
        <p class="text-muted-foreground mb-3 text-sm">{{ 'push.intro' | t }}</p>
        <button tlButton class="w-full" [disabled]="pushBusy()" (click)="enablePush()">
          {{ 'push.enable' | t }}
        </button>
      }
    </tl-card>
    @if (settingsData(); as s) {
      <tl-list class="mt-3" data-testid="push-reminders">
        <tl-list-item
          icon="lucideListChecks"
          [title]="'push.habits' | t"
          [subtitle]="'push.habitsHint' | t"
        >
          <button
            tlListTrailing
            tlCheck
            [checked]="s.pushHabits"
            [attr.aria-label]="'push.habits' | t"
            (click)="saveReminders({ pushHabits: !s.pushHabits })"
          ></button>
        </tl-list-item>
        <tl-list-item
          icon="lucideRuler"
          [title]="'push.checkin' | t"
          [subtitle]="'push.checkinHint' | t"
        >
          <button
            tlListTrailing
            tlCheck
            [checked]="s.pushCheckin"
            [attr.aria-label]="'push.checkin' | t"
            (click)="saveReminders({ pushCheckin: !s.pushCheckin })"
          ></button>
        </tl-list-item>
        <tl-list-item
          icon="lucideGoal"
          [title]="'push.streak' | t"
          [subtitle]="'push.streakHint' | t"
        >
          <button
            tlListTrailing
            tlCheck
            [checked]="s.pushStreak"
            [attr.aria-label]="'push.streak' | t"
            (click)="saveReminders({ pushStreak: !s.pushStreak })"
          ></button>
        </tl-list-item>
      </tl-list>
      <tl-card class="mt-3">
        <div class="flex flex-col gap-4">
          <tl-field [label]="'push.checkinDay' | t">
            <tl-segmented
              [options]="dayOptions()"
              [value]="'' + s.checkinReminderDay"
              (valueChange)="saveReminders({ checkinReminderDay: +$any($event) })"
            />
          </tl-field>
          <div class="grid grid-cols-2 gap-3">
            <tl-field [label]="'push.checkinTime' | t">
              <input
                tlInput
                type="time"
                name="checkinTime"
                [value]="s.checkinReminderTime"
                (change)="saveTime('checkinReminderTime', $event)"
              />
            </tl-field>
            <tl-field [label]="'push.streakTime' | t">
              <input
                tlInput
                type="time"
                name="streakTime"
                [value]="s.streakReminderTime"
                (change)="saveTime('streakReminderTime', $event)"
              />
            </tl-field>
          </div>
        </div>
      </tl-card>
    }

    <tl-section-header [title]="'settings.appearance' | t" />
    <tl-card>
      <tl-segmented
        [options]="themeOptions()"
        [value]="theme.mode()"
        (valueChange)="setTheme($event)"
      />
    </tl-card>

    <tl-section-header [title]="'settings.language' | t" />
    <tl-card>
      <tl-segmented [options]="langOptions" [value]="i18n.lang()" (valueChange)="setLang($event)" />
    </tl-card>

    <tl-section-header [title]="'settings.sync' | t" />
    <tl-card>
      <p class="text-muted-foreground text-sm">{{ 'settings.syncHint' | t }}</p>
      <p class="mt-3 text-sm font-semibold">
        {{ (syncStatusData()?.configured ? 'settings.syncConfigured' : 'settings.syncNone') | t }}
      </p>
      @if (newToken(); as token) {
        <div class="bg-progress/15 mt-3 rounded-2xl p-3">
          <code class="block font-mono text-sm break-all select-all">{{ token }}</code>
          <p class="text-muted-foreground mt-2 text-xs">{{ 'settings.syncWarning' | t }}</p>
          <button tlButton variant="secondary" class="mt-3 w-full" (click)="copyToken(token)">
            {{ 'settings.syncCopy' | t }}
          </button>
        </div>
      }
      <button tlButton variant="secondary" class="mt-4 w-full" (click)="generateToken()">
        {{ 'settings.syncGenerate' | t }}
      </button>
    </tl-card>

    <tl-section-header [title]="'settings.account' | t" />
    <tl-list>
      @if (auth.email(); as email) {
        <tl-list-item icon="lucideMail" [title]="'settings.email' | t: { email }" />
      }
      <button
        tlListItem
        icon="lucideLogOut"
        [title]="'settings.logout' | t"
        [subtitle]="'settings.loggedAs' | t: { user: auth.user() ?? '' }"
        (click)="logout()"
      ></button>
    </tl-list>

    <footer
      class="text-muted-foreground mt-10 flex flex-col items-center gap-2 text-center text-sm"
    >
      <tl-logo [size]="40" wordmark class="text-foreground" />
      <p>{{ 'login.slogan' | t }}</p>
    </footer>

    <app-habit-sheet
      [(open)]="habitSheetOpen"
      [habit]="editingHabit()"
      [newKind]="newHabitKind()"
      [stepsGoal]="settingsData()?.dailyStepsGoal ?? null"
      (saved)="habits.reload()"
    />
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
  /** Local copy, so a dropped habit stays in place while the new order is saved. */
  protected readonly habitOrder = linkedSignal(() => this.habitsData() ?? []);
  protected readonly hasStepsHabit = computed(() =>
    this.habitOrder().some((h) => h.kind === 'STEPS'),
  );
  protected readonly habitSheetOpen = signal(false);
  protected readonly editingHabit = signal<Habit | null>(null);
  protected readonly newHabitKind = signal<HabitKind>('NORMAL');
  protected readonly bodyParts = httpResource<BodyPart[]>(() => '/api/body-parts');
  protected readonly bodyPartsData = safeValue(this.bodyParts);
  protected readonly newBodyPart = signal('');
  protected readonly syncStatus = httpResource<{ configured: boolean }>(
    () => '/api/auth/sync-token',
  );
  protected readonly syncStatusData = safeValue(this.syncStatus);
  protected readonly newToken = signal<string | null>(null);
  private readonly toast = inject(TlToast);

  protected readonly workoutGoal = signal('');
  protected readonly stepsGoal = signal('');
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
    });
  }

  protected readonly push = inject(PushNotifications);
  protected readonly pushBusy = signal(false);

  protected readonly dayOptions = computed<TlSegmentOption[]>(() => {
    this.i18n.lang();
    return [1, 2, 3, 4, 5, 6, 7].map((d) => ({
      value: String(d),
      label: this.i18n.t(`push.day${d}` as TranslationKey),
    }));
  });

  protected async enablePush(): Promise<void> {
    await this.pushAction(() => this.push.enable(), 'push.enabled');
  }

  protected async disablePush(): Promise<void> {
    await this.pushAction(() => this.push.disable(), 'push.disabled');
  }

  protected async testPush(): Promise<void> {
    await this.pushAction(() => this.push.test(), 'push.testSent');
  }

  private async pushAction(
    action: () => Promise<unknown>,
    successKey: TranslationKey,
  ): Promise<void> {
    this.pushBusy.set(true);
    try {
      await action();
      this.toast.success(this.i18n.t(successKey));
    } catch {
      this.toast.error(this.i18n.t('push.error'));
    } finally {
      this.pushBusy.set(false);
    }
  }

  /** Reminder settings are saved right away (REQ-033). */
  protected async saveReminders(patch: Partial<Settings>): Promise<void> {
    const current = this.settingsData();
    if (!current) {
      return;
    }
    try {
      this.settings.set(await this.api.saveSettings({ ...current, ...patch }));
      this.toast.success(this.i18n.t('common.saved'));
    } catch {
      this.toast.error(this.i18n.t('common.saveError'));
    }
  }

  protected saveTime(field: 'checkinReminderTime' | 'streakReminderTime', event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    if (/^\d{2}:\d{2}$/.test(value)) {
      void this.saveReminders({ [field]: value });
    }
  }

  protected openHabit(habit: Habit | null, kind: HabitKind): void {
    this.editingHabit.set(habit);
    this.newHabitKind.set(kind);
    this.habitSheetOpen.set(true);
  }

  protected habitName(h: Habit): string {
    return habitLabel(h, this.settingsData()?.dailyStepsGoal, this.i18n);
  }

  protected time(remindAt: string | null): string | null {
    return habitTime(remindAt);
  }

  protected async dropHabit(event: CdkDragDrop<unknown>): Promise<void> {
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    const list = [...this.habitOrder()];
    moveItemInArray(list, event.previousIndex, event.currentIndex);
    this.habitOrder.set(list);
    if (
      !(await this.attempt(() => this.api.reorderHabits(list.map((h) => h.id)), 'common.saved'))
    ) {
      this.habits.reload();
    }
  }

  protected async removeHabit(habit: Habit): Promise<void> {
    if (
      confirm(this.i18n.t('settings.deleteHabitConfirm', { name: this.habitName(habit) })) &&
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
    if (
      name &&
      name !== part.name &&
      (await this.attempt(() => this.api.renameBodyPart(part.id, name), 'common.saved'))
    ) {
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

  protected async generateToken(): Promise<void> {
    if (this.syncStatusData()?.configured && !confirm(this.i18n.t('settings.syncConfirm'))) {
      return;
    }
    try {
      this.newToken.set((await this.api.newSyncToken()).token);
      this.syncStatus.reload();
    } catch {
      this.toast.error(this.i18n.t('common.saveError'));
    }
  }

  protected async copyToken(token: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(token);
      this.toast.success(this.i18n.t('settings.syncCopied'));
    } catch {
      // clipboard not available: the token is selectable (select-all) for manual copying
    }
  }

  /** Runs a write, shows a green confirmation or a red error toast; true on success. */
  private async attempt(
    action: () => Promise<unknown>,
    successKey: TranslationKey,
  ): Promise<boolean> {
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
        void this.api.saveSettings({ ...current, language: value }).then(
          (s) => this.settings.set(s),
          () => {},
        );
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
