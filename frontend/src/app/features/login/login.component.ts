import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <div class="login-container">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>Order Fulfilment — Login</mat-card-title>
          <mat-card-subtitle>Sign in to access the workflow</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="login()" class="login-form">
            <mat-form-field appearance="outline">
              <mat-label>Username</mat-label>
              <input matInput formControlName="username" placeholder="customer or officer">
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Password</mat-label>
              <input matInput type="password" formControlName="password">
            </mat-form-field>

            @if (error()) {
              <p class="error-msg">Invalid credentials. Please try again.</p>
            }

            <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid">
              Sign In
            </button>
          </form>

          <div class="demo-credentials">
            <p><strong>Demo Credentials:</strong></p>
            <p><code>customer</code> / <code>customer123</code> — Submit & view orders</p>
            <p><code>officer</code> / <code>officer123</code> — Review credit overrides</p>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 80vh;
    }
    .login-card { max-width: 400px; width: 100%; }
    .login-form {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-top: 16px;
    }
    .error-msg { color: #f44336; font-size: 14px; margin: 0; }
    .demo-credentials {
      margin-top: 24px;
      padding: 12px;
      background: #f5f5f5;
      border-radius: 4px;
      font-size: 13px;
    }
    .demo-credentials p { margin: 4px 0; }
    .demo-credentials code { background: #e0e0e0; padding: 2px 6px; border-radius: 3px; }
  `]
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  error = signal(false);

  form = new FormGroup({
    username: new FormControl('', [Validators.required]),
    password: new FormControl('', [Validators.required])
  });

  login(): void {
    if (this.form.invalid) return;

    const { username, password } = this.form.getRawValue();
    const success = this.authService.login(username!, password!);

    if (success) {
      this.error.set(false);
      const role = this.authService.role();
      // Route based on role
      if (role === 'CREDIT_OFFICER') {
        this.router.navigate(['/credit-review']);
      } else {
        this.router.navigate(['/orders']);
      }
    } else {
      this.error.set(true);
    }
  }
}
