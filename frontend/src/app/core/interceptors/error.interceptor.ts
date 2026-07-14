import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

/**
 * Functional HTTP interceptor for global error handling.
 * Logs errors to console; in production, could show a snackbar notification.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error) => {
      const message = error.error?.message || error.message || 'Unknown error';
      console.error(`[HTTP Error] ${req.method} ${req.url} → ${error.status}: ${message}`);
      return throwError(() => error);
    })
  );
};
