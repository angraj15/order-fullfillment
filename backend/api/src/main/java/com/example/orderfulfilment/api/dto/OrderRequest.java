package com.example.orderfulfilment.api.dto;

import com.example.orderfulfilment.service.entity.NotificationPreference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for submitting a new order via the webhook endpoint.
 * Bean Validation annotations are enforced by @Valid in the controller.
 */
@Schema(description = "Order submission request")
public record OrderRequest(

    @NotBlank(message = "customerId is required")
    @Schema(description = "Customer identifier", example = "CUST-001")
    String customerId,

    @NotBlank(message = "customerName is required")
    @Schema(description = "Customer display name", example = "Alice Smith")
    String customerName,

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    @DecimalMax(value = "9999999.99", message = "amount must not exceed 9,999,999.99")
    @Schema(description = "Order amount in EUR", example = "1500.00")
    BigDecimal amount,

    @NotNull(message = "notificationPreference is required")
    @Schema(description = "Notification channel preference", example = "EMAIL")
    NotificationPreference notificationPreference
) {}
