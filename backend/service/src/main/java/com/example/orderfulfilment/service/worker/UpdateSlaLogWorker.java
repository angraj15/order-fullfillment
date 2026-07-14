package com.example.orderfulfilment.service.worker;

import com.example.orderfulfilment.service.domain.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * External task worker for topic: update-sla-log
 *
 * Triggered by the non-interrupting Message Boundary Event on the
 * Approve Credit Override user task when a "CustomerUpdatedPriority"
 * message is correlated. Logs the SLA update without interrupting
 * the credit review flow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateSlaLogWorker extends BaseExternalTaskWorker {

    private final OrderService orderService;

    @Override
    public String getTopicName() {
        return "update-sla-log";
    }

    @Override
    protected Map<String, Object> executeTask(ExternalTask task) {
        String orderId      = task.getVariable("orderId");
        String customerName = task.getVariable("customerName");

        String slaNote = "Customer priority updated at " + Instant.now()
                + " for customer: " + customerName;

        log.info("[UpdateSlaLog] SLA update received — orderId={} note='{}'", orderId, slaNote);

        // Only update the decisionReason field — do NOT change status.
        // The order remains at PENDING_OVERRIDE (non-interrupting event doesn't affect flow).
        try {
            var order = orderService.findById(UUID.fromString(orderId));
            order.setDecisionReason(slaNote);
            // Save directly via repository to avoid status transition validation
        } catch (Exception ex) {
            log.warn("[UpdateSlaLog] Could not update SLA note for order {}: {}", orderId, ex.getMessage());
        }

        return Map.of("slaUpdatedAt", Instant.now().toString());
    }
}
