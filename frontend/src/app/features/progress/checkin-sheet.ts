import { Component, effect, inject, input, model, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TlButton, TlField, TlInput, TlSheet } from '@talia/ui';
import { Api, WeekEntry, addDays, isoDate, parseDecimal } from '../../core/api';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';
import { TranslationKey } from '../../core/i18n/translations';

/** Weekly check-in form: waist, weight, average daily steps. Editing a week replaces its values. */
@Component({
  selector: 'app-checkin-sheet',
  imports: [FormsModule, TlButton, TlField, TlInput, TlSheet, TranslatePipe],
  template: `
    <tl-sheet [(open)]="open" [title]="title()" [closeLabel]="'common.close' | t">
      <form class="flex flex-col gap-4" (ngSubmit)="save()">
        <tl-field [label]="'checkin.date' | t">
          <input tlInput type="date" name="date" required [max]="today" [(ngModel)]="date" />
        </tl-field>
        <div class="grid grid-cols-2 gap-3">
          <tl-field [label]="'checkin.waist' | t" unit="cm">
            <input tlInput name="waist" inputmode="decimal" autocomplete="off" placeholder="78,5" [(ngModel)]="waist" />
          </tl-field>
          <tl-field [label]="'checkin.weight' | t" unit="kg">
            <input tlInput name="weight" inputmode="decimal" autocomplete="off" placeholder="64,2" [(ngModel)]="weight" />
          </tl-field>
        </div>
        <tl-field [label]="'checkin.steps' | t" [hint]="'checkin.stepsHint' | t">
          <input tlInput name="steps" inputmode="numeric" autocomplete="off" placeholder="8 100" [(ngModel)]="steps" />
        </tl-field>

        @if (error(); as key) {
          <p class="text-destructive text-sm" role="alert">{{ key | t }}</p>
        }

        <button tlButton type="submit" class="mt-1 w-full" [disabled]="busy()">{{ 'common.save' | t }}</button>
        @if (entry(); as e) {
          <button tlButton type="button" variant="danger" class="w-full" [disabled]="busy()" (click)="remove(e)">
            {{ 'common.delete' | t }}
          </button>
        }
      </form>
    </tl-sheet>
  `,
})
export class CheckinSheet {
  private readonly api = inject(Api);
  private readonly i18n = inject(I18n);

  readonly open = model(false);
  /** Week being edited, or null for a new check-in. */
  readonly entry = input<WeekEntry | null>(null);
  readonly saved = output<void>();

  protected readonly today = isoDate();
  protected readonly date = signal(this.today);
  protected readonly waist = signal('');
  protected readonly weight = signal('');
  protected readonly steps = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<TranslationKey | null>(null);

  protected title(): string {
    const e = this.entry();
    return e
      ? this.i18n.t('checkin.editTitle', { range: this.i18n.weekRange(e.weekStart, addDays(e.weekStart, 6)) })
      : this.i18n.t('checkin.title');
  }

  constructor() {
    // Prefill whenever the sheet opens.
    effect(() => {
      if (!this.open()) {
        return;
      }
      const e = this.entry();
      this.error.set(null);
      if (e) {
        const sunday = addDays(e.weekStart, 6);
        this.date.set(sunday < this.today ? sunday : this.today);
        this.waist.set(e.waistCm != null ? this.i18n.num(e.waistCm, 1) : '');
        this.weight.set(e.weightKg != null ? this.i18n.num(e.weightKg, 1) : '');
        this.steps.set(e.avgDailySteps != null ? String(e.avgDailySteps) : '');
      } else {
        this.date.set(this.today);
        this.waist.set('');
        this.weight.set('');
        this.steps.set('');
      }
    });
  }

  protected async save(): Promise<void> {
    const waistCm = parseDecimal(this.waist());
    const weightKg = parseDecimal(this.weight());
    const steps = parseDecimal(this.steps());
    if ([waistCm, weightKg, steps].some((v) => Number.isNaN(v))) {
      this.error.set('checkin.invalid');
      return;
    }
    if (waistCm === null && weightKg === null && steps === null) {
      this.error.set('checkin.oneRequired');
      return;
    }
    await this.run(() =>
      this.api.saveCheckin({
        date: this.date(),
        waistCm,
        weightKg,
        avgDailySteps: steps === null ? null : Math.round(steps),
      }),
    );
  }

  protected async remove(entry: WeekEntry): Promise<void> {
    if (confirm(this.i18n.t('checkin.deleteConfirm'))) {
      await this.run(() => this.api.deleteCheckin(entry.weekStart));
    }
  }

  private async run(action: () => Promise<unknown>): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
      this.open.set(false);
      this.saved.emit();
    } catch {
      this.error.set('common.saveError');
    } finally {
      this.busy.set(false);
    }
  }
}
