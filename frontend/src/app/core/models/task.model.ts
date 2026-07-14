export interface TaskResponse {
  taskId: string;
  orderId: string;
  customerName: string;
  amount: number;
  createdAt: string;
}

export interface CompleteTaskRequest {
  approved: boolean;
  comment: string;
}
