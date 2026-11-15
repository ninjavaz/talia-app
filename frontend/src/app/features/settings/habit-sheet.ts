import { Component, computed, effect, inject, input, model, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TlButton, TlField, TlInput, TlSheet, TlToast } from '@talia/ui';
import { Api, Habit, HabitKind } from '../../core/api';
import { habitLabel } from '../../core/habits';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';

/**
 * Add or edit a habit: name and optional time (REQ-010, REQ-011). The steps habit has no editable name (REQ-013),
 * only its time.
 */
@Component({
  selector: 'app-habit-sheet',
  imports: [FormsModule, TlButton, TlField, TlInput, TlSheet, TranslatePipe],
  template: `
    <tl-sheet [(open)]="open" [title]="title()" [closeLabel]="'common.close' | t">
      <form class="flex flex-col gap-4" (ngSubmit)="save()">
        @if (kind() === 'STEPS') {
          <p class="font-semibold">{{ stepsLabel() }}</p>
          <p class="text-muted-foreground -mt-3 text-xs">{{ 'habit.stepsHint' | t }}</p>
        } @else {
          <tl-field [label]="'habit.name' | t">
            <input
              tlInput
              name="name"
              maxlength="100"
              autocomplete="off"
              required
              [(ngModel)]="name"
            />
          </tl-field>
        }
        <tl-field [label]="'habit.time' | t" [hint]="'habit.timeHint' | t">
          <input tlInput type="time" name="time" [(ngModel)]="time" />
        </tl-field>

        @if (error()) {
          <p class="text-destructive text-sm" role="alert">{{ 'common.saveError' | t }}</p>
        }
        <button
          tlButton
          type="submit"
          class="mt-1 w-full"
          [disabled]="busy() || (kind() === 'NORMAL' && !name().trim())"
        >
          {{ 'common.save' | t }}
        </button>
      </form>
    </tl-sheet>
  `,
})
export class HabitSheet {
  private readonly api = inject(Api);
  private readonly i18n = inject(I18n);
  private readonly toast = inject(TlToast);

  readonly open = model(false);
  /** Habit being edited, or null for a new one of `newKind`. */
  readonly habit = input<Habit | null>(null);
  readonly newKind = input<HabitKind>('NORMAL');
  readonly stepsGoal = input<number | null>(null);
  readonly saved = output<void>();

  protected readonly name = signal('');
  protected readonly time = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal(false);

  protected readonly kind = computed<HabitKind>(() => this.habit()?.kind ?? this.newKind());
  protected readonly stepsLabel = computed(() =>
    habitLabel({ name: '', kind: 'STEPS' }, this.stepsGoal(), this.i18n),
  );
  protected readonly title = computed(() =>
    this.i18n.t(this.habit() ? 'habit.editTitle' : 'habit.newTitle'),
  );

  constructor() {
    effect(() => {
      if (this.open()) {
        const h = this.habit();
        this.name.set(h?.kind === 'NORMAL' ? h.name : '');
        this.time.set(h?.remindAt ?? '');
        this.error.set(false);
      }
    });
  }

  protected async save(): Promise<void> {
    const steps = this.kind() === 'STEPS';
    const body = { name: steps ? null : this.name().trim(), remindAt: this.time() || null };
    const current = this.habit();
    this.busy.set(true);
    this.error.set(false);
    try {
      if (current) {
        await this.api.updateHabit(current.id, body);
      } else {
        await this.api.addHabit({ ...body, kind: this.kind() });
      }
      this.open.set(false);
      this.toast.success(this.i18n.t(current ? 'common.saved' : 'common.added'));
      this.saved.emit();
    } catch {
      this.error.set(true);
    } finally {
      this.busy.set(false);
    }
  }
}
