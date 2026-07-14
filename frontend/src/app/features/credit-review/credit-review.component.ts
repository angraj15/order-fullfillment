import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { TaskService } from '../../core/services/task.service';
import { TaskResponse } from '../../core/models/task.model';

@Component({
  selector: 'app-credit-review',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    CurrencyPipe,
    DatePipe
  ],
  templateUrl: './credit-review.component.html'
})
export class CreditReviewComponent implements OnInit {
  private readonly taskService = inject(TaskService);
  private readonly snackBar = inject(MatSnackBar);

  tasks = signal<TaskResponse[]>([]);
  loading = signal(false);

  displayedColumns = ['orderId', 'customerName', 'amount', 'createdAt', 'comment', 'actions'];

  // Per-task comment fields (keyed by taskId)
  comments: Record<string, FormControl<string>> = {};

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading.set(true);
    this.taskService.getCreditOverrideTasks().subscribe({
      next: (tasks) => {
        this.tasks.set(tasks);
        tasks.forEach(t => {
          if (!this.comments[t.taskId]) {
            this.comments[t.taskId] = new FormControl('', { nonNullable: true });
          }
        });
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load credit tasks', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  complete(taskId: string, approved: boolean): void {
    const comment = this.comments[taskId]?.value || '';
    this.taskService.completeTask(taskId, { approved, comment }).subscribe({
      next: () => {
        const action = approved ? 'approved' : 'rejected';
        this.snackBar.open(`Credit override ${action}`, 'Close', { duration: 3000 });
        this.loadTasks();
      },
      error: (err) => {
        const msg = err.error?.message || 'Failed to complete task';
        this.snackBar.open(`Error: ${msg}`, 'Close', { duration: 3000 });
      }
    });
  }
}
