import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'orders', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component')
      .then(m => m.LoginComponent)
  },
  {
    path: 'orders',
    loadComponent: () => import('./features/orders-list/orders-list.component')
      .then(m => m.OrdersListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'new-order',
    loadComponent: () => import('./features/new-order/new-order.component')
      .then(m => m.NewOrderComponent),
    canActivate: [authGuard]
  },
  {
    path: 'credit-review',
    loadComponent: () => import('./features/credit-review/credit-review.component')
      .then(m => m.CreditReviewComponent),
    canActivate: [roleGuard('CREDIT_OFFICER')]
  }
];
