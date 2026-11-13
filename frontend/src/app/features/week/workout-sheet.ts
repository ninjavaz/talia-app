import { Component, effect, inject, model, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TlButton, TlField, TlInput, TlSegmented, TlSheet, TlToast } from '@talia/ui';
import { Api, WorkoutCategory, isoDate } from '../../core/api';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';

const CATEGORIES: WorkoutCategory[] = ['GYM', 'PILATES', 'WALK', 'OTHER'];
const QUICK_DURATIONS = [30, 45, 60, 90];

@Component({
  selector: 'app-workout-sheet',
  imports: [FormsModule, TlButton, TlField, TlInput, TlSegmented, TlSheet, TranslatePipe],
  template: `
    <tl-sheet [(open)]="open" [title]="'week.addWorkout' | t" [closeLabel]="'common.close' | t">
      <form class="flex flex-col gap-4" (ngSubmit)="save()">
        <div class="flex flex-col gap-1.5">
          <span class="text-sm font-medium">{{ 'workout.type' | t }}</span>
          <tl-segmented [options]="categoryOptions()" [value]="category()" (valueChange)="category.set($any($event))" />
        </div>
        <tl-field [label]="'workout.date' | t">
          <input tlInput type="date" name="date" required [max]="today" [(ngModel)]="date" />
        </tl-field>
        <tl-field [label]="'workout.duration' | t" [unit]="'workout.minutes' | t">
          <input tlInput name="duration" inputmode="numeric" required [(ngModel)]="duration" />
        </tl-field>
        <div class="flex gap-2">
          @for (d of quick; track d) {
            <button
              type="button"
              class="flex-1 rounded-full py-2 text-sm font-medium"
              [class]="duration() === '' + d ? 'bg-primary text-primary-foreground' : 'bg-chip text-chip-foreground'"
              (click)="duration.set('' + d)"
            >
              {{ d }}
            </button>
          }
        </div>
        @if (error()) {
          <p class="text-destructive text-sm" role="alert">{{ 'common.saveError' | t }}</p>
        }
        <button tlButton type="submit" class="mt-1 w-full" [disabled]="busy()">{{ 'common.add' | t }}</button>
      </form>
    </tl-sheet>
  `,
})
export class WorkoutSheet {
  private readonly api = inject(Api);
  private readonly i18n = inject(I18n);
  private readonly toast = inject(TlToast);

  readonly open = model(false);
  readonly saved = output<void>();

  protected readonly today = isoDate();
  protected readonly quick = QUICK_DURATIONS;
  protected readonly category = signal<WorkoutCategory>('GYM');
  protected readonly date = signal(this.today);
  protected readonly duration = signal('45');
  protected readonly busy = signal(false);
  protected readonly error = signal(false);

  protected categoryOptions() {
    this.i18n.lang();
    return CATEGORIES.map((c) => ({ value: c, label: this.i18n.t(`workout.${c}`) }));
  }

  constructor() {
    effect(() => {
      if (this.open()) {
        this.date.set(this.today);
        this.error.set(false);
      }
    });
  }

  protected async save(): Promise<void> {
    const durationMin = Number(this.duration());
    if (!Number.isInteger(durationMin) || durationMin < 1 || durationMin > 600) {
      this.error.set(true);
      return;
    }
    this.busy.set(true);
    this.error.set(false);
    try {
      await this.api.addWorkout({ date: this.date(), category: this.category(), durationMin });
      this.open.set(false);
      this.toast.success(this.i18n.t('common.added'));
      this.saved.emit();
    } catch {
      this.error.set(true);
    } finally {
      this.busy.set(false);
    }
  }
}
