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
 * External task worker for topic: send-sms
 *
 * Activated by the OR (inclusive) notification gateway when customer
 * preference is SMS or BOTH.
 * Log-based implementation — no real SMS delivery.
 *
 * Note: The OR gateway join waits for all active notification branches to complete.
 * FULFILLED status update here is a local DB update; the process ends at the
 * Order Fulfilled end event regardless.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SendSmsWorker extends BaseExternalTaskWorker {

    private final OrderService orderService;

    @Override
    public String getTopicName() {
        return "send-sms";
    }

    @Override
    protected Map<String, Object> executeTask(ExternalTask task) {
        String orderId      = task.getVariable("orderId");
        String customerName = task.getVariable("customerName");
        String customerId   = task.getVariable("customerId");

        log.info("[SendSms] Sending fulfilment SMS to customer {} ({}) for orderId={}",
                customerName, customerId, orderId);

        // Simulated SMS send
        log.info("[SendSms] SMS SENT — To: +1-555-{} | Message: 'Order {} fulfilled'",
                customerId.hashCode() % 10000000, orderId);

        // Mark order as FULFILLED in local DB
        // (safe even if SendEmailWorker runs concurrently — optimistic locking handles it)
        try {
            orderService.updateStatus(UUID.fromString(orderId), OrderStatus.FULFILLED,
                    "Order fulfilled — notifications dispatched at " + Instant.now());
        } catch (Exception ex) {
            // FULFILLED may already be set if email worker ran first — log and continue
            log.warn("[SendSms] Could not update FULFILLED status for order {} (may already be set): {}",
                    orderId, ex.getMessage());
        }

        return Map.of("smsSentAt", Instant.now().toString());
    }
}
