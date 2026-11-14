import { Component, effect, inject, input, model, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TlButton, TlField, TlInput, TlSheet, TlToast } from '@talia/ui';
import { Api, PartValue, WeekEntry, addDays, isoDate, parseDecimal } from '../../core/api';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';
import { TranslationKey } from '../../core/i18n/translations';

export interface CheckinPart {
  id: number;
  label: string;
}

/**
 * Weekly check-in: one field per body part the user defined, weight and average daily steps.
 * Saving replaces the week's check-in, so the form always starts from the week's current values.
 */
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
          @for (part of parts(); track part.id) {
            <tl-field [label]="part.label" unit="cm">
              <input
                tlInput
                inputmode="decimal"
                autocomplete="off"
                [name]="'part-' + part.id"
                [ngModel]="partValues()[part.id] ?? ''"
                (ngModelChange)="setPart(part.id, $event)"
              />
            </tl-field>
          }
          <tl-field [label]="'checkin.weight' | t" unit="kg">
            <input tlInput name="weight" inputmode="decimal" autocomplete="off" placeholder="64,2" [(ngModel)]="weight" />
          </tl-field>
        </div>
        <p class="text-muted-foreground -mt-2 text-xs">{{ 'checkin.addPartsHint' | t }}</p>

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
  private readonly toast = inject(TlToast);

  readonly open = model(false);
  /** Body parts to measure, in display order. */
  readonly parts = input<CheckinPart[]>([]);
  /** Week being edited, or null for a new check-in. */
  readonly entry = input<WeekEntry | null>(null);
  readonly saved = output<void>();

  protected readonly today = isoDate();
  protected readonly date = signal(this.today);
  protected readonly partValues = signal<Record<number, string>>({});
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
        this.partValues.set(
          Object.fromEntries(e.measurements.map((m) => [m.partId, this.i18n.num(m.valueCm, 1)])),
        );
        this.weight.set(e.weightKg != null ? this.i18n.num(e.weightKg, 1) : '');
        this.steps.set(e.avgDailySteps != null ? String(e.avgDailySteps) : '');
      } else {
        this.date.set(this.today);
        this.partValues.set({});
        this.weight.set('');
        this.steps.set('');
      }
    });
  }

  protected setPart(id: number, value: string): void {
    this.partValues.update((values) => ({ ...values, [id]: value }));
  }

  protected async save(): Promise<void> {
    const measurements: PartValue[] = [];
    for (const part of this.parts()) {
      const value = parseDecimal(this.partValues()[part.id]);
      if (Number.isNaN(value)) {
        this.error.set('checkin.invalid');
        return;
      }
      if (value !== null) {
        measurements.push({ partId: part.id, valueCm: value });
      }
    }
    const weightKg = parseDecimal(this.weight());
    const steps = parseDecimal(this.steps());
    if (Number.isNaN(weightKg) || Number.isNaN(steps)) {
      this.error.set('checkin.invalid');
      return;
    }
    if (!measurements.length && weightKg === null && steps === null) {
      this.error.set('checkin.oneRequired');
      return;
    }
    await this.run(
      () =>
        this.api.saveCheckin({
          date: this.date(),
          measurements,
          weightKg,
          avgDailySteps: steps === null ? null : Math.round(steps),
        }),
      'common.saved',
    );
  }

  protected async remove(entry: WeekEntry): Promise<void> {
    if (confirm(this.i18n.t('checkin.deleteConfirm'))) {
      await this.run(() => this.api.deleteCheckin(entry.weekStart), 'common.deleted');
    }
  }

  private async run(action: () => Promise<unknown>, successKey: TranslationKey): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
      this.open.set(false);
      this.toast.success(this.i18n.t(successKey));
      this.saved.emit();
    } catch {
      this.error.set('common.saveError');
    } finally {
      this.busy.set(false);
    }
  }
}
