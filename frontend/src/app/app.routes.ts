import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth';
import { Shell } from './layout/shell';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login').then((m) => m.LoginPage),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'progress' },
      {
        path: 'progress',
        loadComponent: () => import('./features/progress/progress').then((m) => m.ProgressPage),
      },
      {
        path: 'week',
        loadComponent: () => import('./features/week/week').then((m) => m.WeekPage),
      },
      {
        path: 'summary',
        loadComponent: () => import('./features/summary/summary').then((m) => m.SummaryPage),
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/settings/settings').then((m) => m.SettingsPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
