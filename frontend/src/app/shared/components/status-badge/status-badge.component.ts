import { Component, computed, input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { OrderStatus } from '../../../core/models/order.model';

const STATUS_COLORS: Record<OrderStatus, string> = {
  RECEIVED: '#2196F3',
  VALIDATING: '#03A9F4',
  AUTO_APPROVED: '#4CAF50',
  PENDING_OVERRIDE: '#FF9800',
  APPROVED: '#8BC34A',
  REJECTED: '#F44336',
  AUTO_CANCELLED: '#9E9E9E',
  FULFILLED: '#4CAF50'
};

const STATUS_LABELS: Record<OrderStatus, string> = {
  RECEIVED: 'Received',
  VALIDATING: 'Validating',
  AUTO_APPROVED: 'Auto Approved',
  PENDING_OVERRIDE: 'Pending Override',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  AUTO_CANCELLED: 'Auto Cancelled',
  FULFILLED: 'Fulfilled'
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [MatChipsModule],
  template: `
    <mat-chip [style.background-color]="color()" style="color: white; font-size: 12px;">
      {{ label() }}
    </mat-chip>
  `
})
export class StatusBadgeComponent {
  status = input.required<OrderStatus>();
  color = computed(() => STATUS_COLORS[this.status()] ?? '#757575');
  label = computed(() => STATUS_LABELS[this.status()] ?? this.status());
}
