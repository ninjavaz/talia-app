import { diamondBurst } from './diamonds';

describe('diamondBurst', () => {
  it('cleans up immediately when animations are not available', () => {
    const origin = document.createElement('button');
    document.body.appendChild(origin);
    diamondBurst(origin, { count: 5 });
    // jsdom has no Element.animate, so the layer must not be left behind
    expect(document.body.querySelectorAll('[aria-hidden="true"]').length).toBe(0);
  });
});
