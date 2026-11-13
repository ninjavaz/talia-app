import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TlRing } from './ring';

@Component({
  imports: [TlRing],
  template: `<tl-ring [value]="value" [max]="100" [size]="100" [stroke]="10" />`,
})
class Host {
  value = 50;
}

describe('TlRing', () => {
  it('draws the value as a fraction of the 270° arc', async () => {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    const circles = fixture.nativeElement.querySelectorAll('circle');
    const arc = (2 * Math.PI * 45 * 270) / 360;
    const track = parseFloat(circles[0].getAttribute('stroke-dasharray'));
    const value = parseFloat(circles[1].getAttribute('stroke-dasharray'));
    expect(track).toBeCloseTo(arc, 3);
    expect(value).toBeCloseTo(arc / 2, 3);
  });

  it('clamps values above max', async () => {
    const fixture = TestBed.createComponent(Host);
    fixture.componentInstance.value = 250;
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('tl-ring').getAttribute('aria-valuenow')).toBe('250');
    const circles = fixture.nativeElement.querySelectorAll('circle');
    expect(circles[1].getAttribute('stroke-dasharray')).toBe(circles[0].getAttribute('stroke-dasharray'));
  });
});
