import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TlChipSelect } from './chip-select';

@Component({
  imports: [TlChipSelect],
  template: `<tl-chip-select [options]="options()" [(value)]="value" />`,
})
class Host {
  readonly options = signal(
    Array.from({ length: 12 }, (_, i) => ({ value: `part-${i}`, label: `Obwód numer ${i}` })),
  );
  readonly value = signal('part-0');
}

describe('TlChipSelect', () => {
  it('renders every option as its own chip in one scrollable row', async () => {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    const row: HTMLElement = fixture.nativeElement.querySelector('tl-chip-select');
    expect(row.classList).toContain('overflow-x-auto');
    expect(row.style.gridTemplateColumns).toBe('');
    const chips = row.querySelectorAll('button[role="radio"]');
    expect(chips.length).toBe(12);
    chips.forEach((chip) => expect(chip.classList).toContain('shrink-0'));
  });

  it('selects an option on click', async () => {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    const chips: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('button');
    chips[7].click();
    await fixture.whenStable();
    expect(fixture.componentInstance.value()).toBe('part-7');
    expect(chips[7].getAttribute('aria-checked')).toBe('true');
    expect(chips[0].getAttribute('aria-checked')).toBe('false');
  });
});
