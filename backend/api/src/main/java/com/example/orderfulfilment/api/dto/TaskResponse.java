package com.example.orderfulfilment.api.dto;

import com.example.orderfulfilment.service.domain.CreditTaskService.CreditTaskView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a pending credit-override user task.
 * Combines Camunda task metadata with enriched local order data.
 */
@Schema(description = "Pending credit override task")
public record TaskResponse(

    @Schema(description = "Camunda task ID") String taskId,
    @Schema(description = "Order UUID") UUID orderId,
    @Schema(description = "Customer display name") String customerName,
    @Schema(description = "Order amount in EUR") BigDecimal amount,
    @Schema(description = "Task creation timestamp (UTC)") Instant createdAt
) {
    public static TaskResponse from(CreditTaskView view) {
        Instant created = view.task().getCreated() != null
                ? view.task().getCreated().toInstant()
                : Instant.now();
        return new TaskResponse(
            view.task().getId(),
            view.order().getId(),
            view.order().getCustomerName(),
            view.order().getAmount(),
            created
        );
    }
}
