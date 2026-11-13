import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { I18n } from './core/i18n/i18n';
import { Theme } from './core/theme/theme';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
export class App {
  constructor() {
    // Instantiate eagerly so theme and <html lang> are applied on startup.
    inject(Theme);
    inject(I18n);
  }
}
