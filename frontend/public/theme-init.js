// Applies the theme before the first paint to avoid a light flash in dark mode.
// A separate file (not inline) so the Content-Security-Policy can forbid inline scripts.
try {
  var m = localStorage.getItem('talia.theme') || 'system';
  if (m === 'dark' || (m === 'system' && matchMedia('(prefers-color-scheme: dark)').matches)) {
    document.documentElement.classList.add('dark');
  }
} catch (e) {}
