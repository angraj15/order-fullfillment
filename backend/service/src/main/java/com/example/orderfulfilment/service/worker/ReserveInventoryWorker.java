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
 * External task worker for topic: reserve-inventory
 *
 * Runs in parallel with GenerateInvoiceWorker after credit is secured.
 * Simulates inventory reservation — log-based implementation.
 * Updates order status to reflect fulfilment progress.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReserveInventoryWorker extends BaseExternalTaskWorker {

    private final OrderService orderService;

    @Override
    public String getTopicName() {
        return "reserve-inventory";
    }

    @Override
    protected Map<String, Object> executeTask(ExternalTask task) {
        String orderId      = task.getVariable("orderId");
        String customerId   = task.getVariable("customerId");
        Double amount       = task.getVariable("amount");

        log.info("[ReserveInventory] Reserving inventory for orderId={} customerId={} amount={}",
                orderId, customerId, amount);

        // Simulated reservation — in production this would call inventory service
        String reservationRef = "INV-" + orderId.substring(0, 8).toUpperCase();
        log.info("[ReserveInventory] Inventory reserved — ref={} orderId={}", reservationRef, orderId);

        // Update order status to AUTO_APPROVED (or leave as APPROVED if officer already approved)
        try {
            OrderStatus currentStatus = orderService.findById(UUID.fromString(orderId)).getStatus();
            if (currentStatus == OrderStatus.VALIDATING) {
                orderService.updateStatus(UUID.fromString(orderId), OrderStatus.AUTO_APPROVED,
                        "Credit auto-approved: amount <= 1000");
            }
        } catch (Exception ex) {
            log.warn("[ReserveInventory] Could not update order status for {}: {}", orderId, ex.getMessage());
        }

        return Map.of(
                "inventoryReservationRef", reservationRef,
                "inventoryReserved", true
        );
    }
}
