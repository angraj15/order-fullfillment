import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CompleteTaskRequest, TaskResponse } from '../models/task.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/tasks/credit-override`;

  getCreditOverrideTasks(): Observable<TaskResponse[]> {
    return this.http.get<TaskResponse[]>(this.baseUrl);
  }

  completeTask(taskId: string, request: CompleteTaskRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${taskId}/complete`, request);
  }
}
