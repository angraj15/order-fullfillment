package com.example.orderfulfilment.service.worker;

import com.example.orderfulfilment.service.domain.OrderService;
import com.example.orderfulfilment.service.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * External task worker for topic: validate-order
 *
 * Validates the order payload received via process variables.
 * Sets validationResult (VALID/INVALID) and validationReason.
 * Updates local order status to VALIDATING (valid) or REJECTED (invalid).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateOrderWorker extends BaseExternalTaskWorker {

    private final OrderService orderService;

    @Override
    public String getTopicName() {
        return "validate-order";
    }

    @Override
    protected Map<String, Object> executeTask(ExternalTask task) {
        String orderId      = task.getVariable("orderId");
        String customerId   = task.getVariable("customerId");
        String customerName = task.getVariable("customerName");
        Double amount       = task.getVariable("amount");

        log.debug("[ValidateOrder] orderId={} customerId={} amount={}", orderId, customerId, amount);

        ValidationResult result = validate(customerId, customerName, amount);

        // Determine the correct post-validation status:
        // - Invalid → REJECTED
        // - Valid, amount <= 1000 → VALIDATING (auto-approved path; ReserveInventoryWorker will advance to AUTO_APPROVED)
        // - Valid, amount > 1000 → PENDING_OVERRIDE (credit officer must approve)
        OrderStatus newStatus;
        String reason;
        if (!result.isValid()) {
            newStatus = OrderStatus.REJECTED;
            reason = result.reason();
        } else if (amount != null && amount > 1000) {
            newStatus = OrderStatus.PENDING_OVERRIDE;
            reason = "Credit override required: amount " + amount + " > 1000";
        } else {
            newStatus = OrderStatus.VALIDATING;
            reason = result.reason();
        }
        orderService.updateStatus(UUID.fromString(orderId), newStatus, reason);

        log.info("[ValidateOrder] orderId={} result={} reason='{}'",
                orderId, result.isValid() ? "VALID" : "INVALID", result.reason());

        return Map.of(
                "validationResult", result.isValid() ? "VALID" : "INVALID",
                "validationReason", result.reason() != null ? result.reason() : ""
        );
    }

    // -------------------------------------------------------------------------
    // Validation logic — kept package-friendly for unit testing
    // -------------------------------------------------------------------------

    ValidationResult validate(String customerId, String customerName, Double amount) {
        if (customerId == null || customerId.isBlank()) {
            return ValidationResult.invalid("customerId is required");
        }
        if (customerName == null || customerName.isBlank()) {
            return ValidationResult.invalid("customerName is required");
        }
        if (amount == null || amount <= 0) {
            return ValidationResult.invalid("amount must be a positive value");
        }
        return ValidationResult.valid();
    }

    // -------------------------------------------------------------------------
    // Result value object
    // -------------------------------------------------------------------------

    public record ValidationResult(boolean isValid, String reason) {
        static ValidationResult valid()              { return new ValidationResult(true, "Order is valid"); }
        static ValidationResult invalid(String why)  { return new ValidationResult(false, why); }
    }
}
