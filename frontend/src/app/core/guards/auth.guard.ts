import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, UserRole } from '../services/auth.service';

/**
 * Route guard that checks if user is logged in.
 * Redirects to /login if not authenticated.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }
  return router.createUrlTree(['/login']);
};

/**
 * Route guard that checks if user has a specific role.
 * Usage: canActivate: [roleGuard('CREDIT_OFFICER')]
 */
export function roleGuard(requiredRole: UserRole): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isLoggedIn()) {
      return router.createUrlTree(['/login']);
    }
    if (authService.role() !== requiredRole) {
      // Redirect to home if wrong role
      return router.createUrlTree(['/orders']);
    }
    return true;
  };
}
