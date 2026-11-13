import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideLogOut, lucideMonitor, lucideMoon, lucideSun } from '@ng-icons/lucide';
import { HlmButtonImports } from '@ui/button';
import { HlmCardImports } from '@ui/card';
import { HlmToggleGroupImports } from '@ui/toggle-group';
import { Auth } from '../../core/auth/auth';
import { I18n, TranslatePipe } from '../../core/i18n/i18n';
import { Lang } from '../../core/i18n/translations';
import { Theme, ThemeMode } from '../../core/theme/theme';

@Component({
  selector: 'app-settings',
  imports: [HlmButtonImports, HlmCardImports, HlmToggleGroupImports, NgIcon, TranslatePipe],
  viewProviders: [provideIcons({ lucideSun, lucideMoon, lucideMonitor, lucideLogOut })],
  template: `
    <h1 class="mb-4 text-2xl font-bold">{{ 'settings.title' | t }}</h1>

    <div class="flex flex-col gap-4">
      <section hlmCard>
        <div hlmCardHeader>
          <h2 hlmCardTitle>{{ 'settings.appearance' | t }}</h2>
        </div>
        <div hlmCardContent>
          <hlm-toggle-group
            type="single"
            variant="outline"
            class="w-full"
            [value]="theme.mode()"
            (valueChange)="setTheme($event)"
          >
            <button hlmToggleGroupItem value="system" class="flex-1">
              <ng-icon name="lucideMonitor" /> {{ 'settings.theme.system' | t }}
            </button>
            <button hlmToggleGroupItem value="light" class="flex-1">
              <ng-icon name="lucideSun" /> {{ 'settings.theme.light' | t }}
            </button>
            <button hlmToggleGroupItem value="dark" class="flex-1">
              <ng-icon name="lucideMoon" /> {{ 'settings.theme.dark' | t }}
            </button>
          </hlm-toggle-group>
        </div>
      </section>

      <section hlmCard>
        <div hlmCardHeader>
          <h2 hlmCardTitle>{{ 'settings.language' | t }}</h2>
        </div>
        <div hlmCardContent>
          <hlm-toggle-group
            type="single"
            variant="outline"
            class="w-full"
            [value]="i18n.lang()"
            (valueChange)="setLang($event)"
          >
            <button hlmToggleGroupItem value="pl" class="flex-1">Polski</button>
            <button hlmToggleGroupItem value="en" class="flex-1">English</button>
          </hlm-toggle-group>
        </div>
      </section>

      <section hlmCard>
        <div hlmCardHeader>
          <h2 hlmCardTitle>{{ 'settings.goals' | t }}</h2>
          <p hlmCardDescription>{{ 'settings.goalsSoon' | t }}</p>
        </div>
      </section>

      <section hlmCard>
        <div hlmCardHeader>
          <h2 hlmCardTitle>{{ 'settings.account' | t }}</h2>
          <p hlmCardDescription>{{ 'settings.loggedAs' | t: { user: auth.user() ?? '' } }}</p>
        </div>
        <div hlmCardContent>
          <button hlmBtn variant="outline" class="w-full" (click)="logout()">
            <ng-icon name="lucideLogOut" /> {{ 'settings.logout' | t }}
          </button>
        </div>
      </section>
    </div>
  `,
})
export class SettingsPage {
  protected readonly theme = inject(Theme);
  protected readonly i18n = inject(I18n);
  protected readonly auth = inject(Auth);
  private readonly router = inject(Router);

  protected setTheme(value: unknown): void {
    // single toggle group emits undefined when the active item is clicked again - keep the current one
    if (value === 'system' || value === 'light' || value === 'dark') {
      this.theme.setMode(value as ThemeMode);
    }
  }

  protected setLang(value: unknown): void {
    if (value === 'pl' || value === 'en') {
      this.i18n.setLang(value as Lang);
    }
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigate(['/login']);
  }
}
