import { Component, inject, OnInit, signal } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { OrderService } from '../../core/services/order.service';
import { OrderResponse } from '../../core/models/order.model';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    DatePipe,
    CurrencyPipe,
    StatusBadgeComponent
  ],
  templateUrl: './orders-list.component.html'
})
export class OrdersListComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly snackBar = inject(MatSnackBar);

  orders = signal<OrderResponse[]>([]);
  loading = signal(false);

  displayedColumns = ['id', 'customerName', 'amount', 'status', 'createdAt', 'actions'];

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.orderService.getOrders().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load orders', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  updatePriority(orderId: string): void {
    this.orderService.updatePriority(orderId).subscribe({
      next: () => {
        this.snackBar.open('Priority update sent!', 'Close', { duration: 3000 });
        this.loadOrders();
      },
      error: (err) => {
        const msg = err.error?.message || 'Failed to send priority update';
        this.snackBar.open(`Error: ${msg}`, 'Close', { duration: 3000 });
      }
    });
  }
}
