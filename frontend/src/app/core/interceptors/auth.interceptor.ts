import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Attaches Basic Auth header from the logged-in user's credentials
 * to all outgoing API requests.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const credentials = authService.credentials();

  if (credentials && req.url.includes('/api/')) {
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Basic ${credentials}`
      }
    });
    return next(authReq);
  }
  return next(req);
};
