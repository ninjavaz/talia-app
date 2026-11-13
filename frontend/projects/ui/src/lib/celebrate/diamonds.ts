export interface DiamondBurstOptions {
  /** Number of diamonds (default 14). */
  count?: number;
  /** Horizontal spread in px (default 140). */
  spread?: number;
  /** How high they fly in px (default 150). */
  height?: number;
}

/**
 * Confetti made only of diamonds 💎, bursting from the centre of `origin`.
 * Dependency-free (Web Animations API), cleans up after itself and respects "reduce motion".
 */
export function diamondBurst(origin: Element, options: DiamondBurstOptions = {}): void {
  const doc = origin.ownerDocument;
  const win = doc.defaultView;
  if (!win || win.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
    return;
  }
  const { count = 14, spread = 140, height = 150 } = options;
  const rect = origin.getBoundingClientRect();
  const x0 = rect.left + rect.width / 2;
  const y0 = rect.top + rect.height / 2;

  const layer = doc.createElement('div');
  layer.setAttribute('aria-hidden', 'true');
  layer.style.cssText = 'position:fixed;inset:0;pointer-events:none;z-index:80;overflow:hidden';
  doc.body.appendChild(layer);

  let running = 0;
  for (let i = 0; i < count; i++) {
    const gem = doc.createElement('span');
    gem.textContent = '💎';
    const size = 12 + Math.random() * 14;
    gem.style.cssText =
      `position:absolute;left:${x0}px;top:${y0}px;font-size:${size}px;line-height:1;` +
      'transform:translate(-50%,-50%);will-change:transform,opacity';
    layer.appendChild(gem);

    if (typeof gem.animate !== 'function') {
      continue;
    }
    const dx = (Math.random() - 0.5) * 2 * spread;
    const up = height * (0.55 + Math.random() * 0.6);
    const fall = 80 + Math.random() * 120;
    const spin = (Math.random() - 0.5) * 720;
    running++;
    gem
      .animate(
        [
          { transform: 'translate(-50%,-50%) scale(0.3) rotate(0deg)', opacity: 1 },
          {
            transform: `translate(calc(-50% + ${dx * 0.6}px), calc(-50% - ${up}px)) scale(1) rotate(${spin / 2}deg)`,
            opacity: 1,
            offset: 0.45,
          },
          {
            transform: `translate(calc(-50% + ${dx}px), calc(-50% - ${up - fall}px)) scale(0.8) rotate(${spin}deg)`,
            opacity: 0,
          },
        ],
        {
          duration: 900 + Math.random() * 500,
          easing: 'cubic-bezier(0.2, 0.7, 0.4, 1)',
          delay: Math.random() * 80,
          fill: 'forwards',
        },
      )
      .finished.catch(() => undefined)
      .finally(() => {
        if (--running === 0) {
          layer.remove();
        }
      });
  }
  if (running === 0) {
    layer.remove();
  }
}
