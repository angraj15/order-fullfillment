package com.example.orderfulfilment.api.dto;

import com.example.orderfulfilment.service.entity.NotificationPreference;
import com.example.orderfulfilment.service.entity.Order;
import com.example.orderfulfilment.service.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for order data. Produced from the Order entity via OrderMapper.
 * Record-based — immutable, no boilerplate.
 */
@Schema(description = "Order details response")
public record OrderResponse(

    @Schema(description = "Order UUID") UUID id,
    @Schema(description = "Customer identifier") String customerId,
    @Schema(description = "Customer display name") String customerName,
    @Schema(description = "Order amount in EUR") BigDecimal amount,
    @Schema(description = "Notification channel preference") NotificationPreference notificationPreference,
    @Schema(description = "Current order status") OrderStatus status,
    @Schema(description = "Camunda process instance ID") String processInstanceId,
    @Schema(description = "Reason for current status (approval/rejection/cancellation)") String decisionReason,
    @Schema(description = "Order creation timestamp (UTC)") Instant createdAt,
    @Schema(description = "Last status update timestamp (UTC)") Instant updatedAt
) {
    /** Convenience factory from entity. */
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getCustomerName(),
            order.getAmount(),
            order.getNotificationPreference(),
            order.getStatus(),
            order.getProcessInstanceId(),
            order.getDecisionReason(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
