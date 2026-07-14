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
 * External task worker for topic: auto-cancel-order
 *
 * Triggered when the Timer Boundary Event (interrupting) fires on the
 * Approve Credit Override user task. This means no action was taken
 * within the configured timeout (PT2M in dev, PT10M default).
 *
 * Updates the local order status to AUTO_CANCELLED.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoCancelWorker extends BaseExternalTaskWorker {

    private final OrderService orderService;

    @Override
    public String getTopicName() {
        return "auto-cancel-order";
    }

    @Override
    protected Map<String, Object> executeTask(ExternalTask task) {
        String orderId = task.getVariable("orderId");

        log.info("[AutoCancel] Timer expired — auto-cancelling orderId={}", orderId);

        orderService.updateStatus(UUID.fromString(orderId), OrderStatus.AUTO_CANCELLED,
                "Order auto-cancelled: credit override not actioned within timeout at " + Instant.now());

        log.info("[AutoCancel] Order {} status set to AUTO_CANCELLED", orderId);

        return Map.of("autoCancelledAt", Instant.now().toString());
    }
}
