import { Injectable, signal, computed } from '@angular/core';
import { Router } from '@angular/router';

export type UserRole = 'CUSTOMER' | 'CREDIT_OFFICER' | null;

export interface AuthUser {
  username: string;
  role: UserRole;
  credentials: string; // Base64-encoded "username:password"
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUser = signal<AuthUser | null>(this.loadFromSession());

  user = this.currentUser.asReadonly();
  isLoggedIn = computed(() => this.currentUser() !== null);
  role = computed(() => this.currentUser()?.role ?? null);
  credentials = computed(() => this.currentUser()?.credentials ?? null);

  constructor(private router: Router) {}

  /**
   * Attempt login. Validates credentials against known demo users.
   * Returns true on success, false on failure.
   */
  login(username: string, password: string): boolean {
    const role = this.resolveRole(username, password);
    if (!role) {
      return false;
    }

    const user: AuthUser = {
      username,
      role,
      credentials: btoa(`${username}:${password}`)
    };

    this.currentUser.set(user);
    sessionStorage.setItem('auth_user', JSON.stringify(user));
    return true;
  }

  logout(): void {
    this.currentUser.set(null);
    sessionStorage.removeItem('auth_user');
    this.router.navigate(['/login']);
  }

  private resolveRole(username: string, password: string): UserRole {
    // Demo users matching SecurityConfig.java
    if (username === 'customer' && password === 'customer123') return 'CUSTOMER';
    if (username === 'officer' && password === 'officer123') return 'CREDIT_OFFICER';
    if (username === 'admin' && password === 'admin123') return 'CUSTOMER'; // admin defaults to customer view
    return null;
  }

  private loadFromSession(): AuthUser | null {
    const stored = sessionStorage.getItem('auth_user');
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch {
        return null;
      }
    }
    return null;
  }
}
