import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { NgIcon } from '@ng-icons/core';

/** White card with divided rows. Put `tl-list-item`s (or links/buttons with `tlListItem`) inside. */
@Component({
  selector: 'tl-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'bg-card text-card-foreground flex flex-col divide-y divide-border rounded-[1.75rem] px-4 shadow-sm',
  },
  template: `<ng-content />`,
})
export class TlList {}

/**
 * Row with an icon chip, title, optional subtitle and a trailing slot (`tlListTrailing`).
 * The icon name must be registered with `provideIcons` (the app registers its icons globally).
 *
 * <a tlListItem routerLink="/x" icon="lucideRuler" title="Talia" subtitle="78 cm">
 *   <span tlListTrailing>...</span>
 * </a>
 */
@Component({
  selector: 'tl-list-item, a[tlListItem], button[tlListItem]',
  imports: [NgIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class:
      'flex w-full items-center gap-3.5 py-3.5 text-left disabled:opacity-50 [a&]:active:opacity-70 [button&]:active:opacity-70',
  },
  template: `
    @if (icon(); as name) {
      <span
        class="bg-chip text-chip-foreground flex size-11 shrink-0 items-center justify-center rounded-2xl text-xl"
      >
        <ng-icon [name]="name" />
      </span>
    }
    <span class="min-w-0 flex-1">
      <span class="block truncate font-semibold">{{ title() }}</span>
      @if (subtitle(); as sub) {
        <span class="text-muted-foreground block truncate text-sm">{{ sub }}</span>
      }
      <ng-content />
    </span>
    <ng-content select="[tlListTrailing]" />
  `,
})
export class TlListItem {
  readonly title = input.required<string>();
  readonly subtitle = input<string | null>(null);
  readonly icon = input<string | null>(null);
}
