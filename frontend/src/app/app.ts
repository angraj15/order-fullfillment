import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatTabsModule, MatButtonModule, MatIconModule
  ],
  template: `
    <mat-toolbar color="primary">
      <span>Order Fulfilment Workflow</span>
      <span class="spacer"></span>
      @if (authService.isLoggedIn()) {
        <span class="user-info">
          {{ authService.user()?.username }} ({{ authService.user()?.role }})
        </span>
        <button mat-icon-button (click)="authService.logout()" title="Logout" aria-label="Logout">
          <mat-icon>logout</mat-icon>
        </button>
      }
    </mat-toolbar>

    @if (authService.isLoggedIn()) {
      <nav mat-tab-nav-bar [tabPanel]="tabPanel">
        @if (authService.role() === 'CUSTOMER') {
          <a mat-tab-link routerLink="/orders" routerLinkActive #rla1="routerLinkActive" [active]="rla1.isActive">
            Orders
          </a>
          <a mat-tab-link routerLink="/new-order" routerLinkActive #rla2="routerLinkActive" [active]="rla2.isActive">
            New Order
          </a>
        }
        @if (authService.role() === 'CREDIT_OFFICER') {
          <a mat-tab-link routerLink="/credit-review" routerLinkActive #rla3="routerLinkActive" [active]="rla3.isActive">
            Credit Review
          </a>
        }
      </nav>
    }

    <mat-tab-nav-panel #tabPanel>
      <div class="content">
        <router-outlet />
      </div>
    </mat-tab-nav-panel>
  `,
  styles: [`
    .content { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .spacer { flex: 1; }
    .user-info { font-size: 14px; margin-right: 8px; opacity: 0.9; }
  `]
})
export class App {
  authService = inject(AuthService);
}
