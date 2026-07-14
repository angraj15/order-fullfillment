import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrderService } from '../../core/services/order.service';
import { NotificationPreference } from '../../core/models/order.model';

@Component({
  selector: 'app-new-order',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCardModule,
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './new-order.component.html'
})
export class NewOrderComponent {
  private readonly orderService = inject(OrderService);
  private readonly snackBar = inject(MatSnackBar);

  submitting = signal(false);

  form = new FormGroup({
    customerId: new FormControl('', [Validators.required]),
    customerName: new FormControl('', [Validators.required]),
    amount: new FormControl<number | null>(null, [Validators.required, Validators.min(0.01)]),
    notificationPreference: new FormControl<NotificationPreference>('EMAIL', [Validators.required])
  });

  preferences: NotificationPreference[] = ['EMAIL', 'SMS', 'BOTH'];

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const value = this.form.getRawValue();

    this.orderService.createOrder({
      customerId: value.customerId!,
      customerName: value.customerName!,
      amount: value.amount!,
      notificationPreference: value.notificationPreference!
    }).subscribe({
      next: (order) => {
        this.snackBar.open(`Order created: ${order.id.substring(0, 8)}... Status: ${order.status}`, 'Close', { duration: 5000 });
        this.form.reset({ notificationPreference: 'EMAIL' });
        this.submitting.set(false);
      },
      error: (err) => {
        const msg = err.error?.message || 'Failed to create order';
        this.snackBar.open(`Error: ${msg}`, 'Close', { duration: 5000, panelClass: 'error-snackbar' });
        this.submitting.set(false);
      }
    });
  }
}
