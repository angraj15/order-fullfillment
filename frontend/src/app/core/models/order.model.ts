export type OrderStatus =
  | 'RECEIVED'
  | 'VALIDATING'
  | 'AUTO_APPROVED'
  | 'PENDING_OVERRIDE'
  | 'APPROVED'
  | 'REJECTED'
  | 'AUTO_CANCELLED'
  | 'FULFILLED';

export type NotificationPreference = 'EMAIL' | 'SMS' | 'BOTH';

export interface OrderRequest {
  customerId: string;
  customerName: string;
  amount: number;
  notificationPreference: NotificationPreference;
}

export interface OrderResponse {
  id: string;
  customerId: string;
  customerName: string;
  amount: number;
  notificationPreference: NotificationPreference;
  status: OrderStatus;
  processInstanceId: string;
  decisionReason?: string;
  createdAt: string;
  updatedAt: string;
}
