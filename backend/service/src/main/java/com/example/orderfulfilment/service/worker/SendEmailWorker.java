package com.example.orderfulfilment.service.worker;

import com.example.orderfulfilment.service.domain.OrderService;
import com.example.orderfulfilment.service.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * External task worker for topic: send-email
 *
 * Activated by the OR (inclusive) notification gateway when customer
 * preference is EMAIL or BOTH.
 * Log-based implementation — no real email delivery.
 * For EMAIL-only orders this is the last active notification branch,
 * so it also marks the order FULFILLED in the local DB.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SendEmailWorker extends BaseExternalTaskWorker {

    private final OrderService orderService;

    @Override
    public String getTopicName() {
        return "send-email";
    }

    @Override
    protected Map<String, Object> executeTask(ExternalTask task) {
        String orderId      = task.getVariable("orderId");
        String customerName = task.getVariable("customerName");
        String customerId   = task.getVariable("customerId");
        String invoiceNumber = task.getVariable("invoiceNumber");

        log.info("[SendEmail] Sending fulfilment email to customer {} ({}) for orderId={} invoice={}",
                customerName, customerId, orderId, invoiceNumber);

        // Simulated email send
        log.info("[SendEmail] EMAIL SENT — Subject: 'Your order {} has been fulfilled' | To: {}@example.com",
                orderId, customerId != null ? customerId.toLowerCase() : "unknown");

        // Mark order as FULFILLED
        try {
            orderService.updateStatus(UUID.fromString(orderId), OrderStatus.FULFILLED,
                    "Order fulfilled — email notification sent at " + Instant.now());
        } catch (Exception ex) {
            // May already be FULFILLED if SMS worker ran first — safe to ignore
            log.debug("[SendEmail] Status update skipped for order {}: {}", orderId, ex.getMessage());
        }

        return Map.of(
                "emailSentAt", Instant.now().toString(),
                "emailRecipient", (customerId != null ? customerId : "unknown") + "@example.com"
        );
    }
}
