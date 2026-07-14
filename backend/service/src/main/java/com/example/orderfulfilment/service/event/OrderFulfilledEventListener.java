package com.example.orderfulfilment.service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens to OrderFulfilledEvent and logs fulfilment.
 *
 * In a production system this would publish to Kafka, send audit records, etc.
 * Demonstrates the Observer pattern — fully decoupled from OrderService.
 */
@Component
@Slf4j
public class OrderFulfilledEventListener {

    @EventListener
    @Async
    public void onOrderFulfilled(OrderFulfilledEvent event) {
        log.info("[DOMAIN EVENT] OrderFulfilled — orderId={} customerId={} customerName={} amount={}",
                event.getOrderId(),
                event.getCustomerId(),
                event.getCustomerName(),
                event.getAmount());
        // Stretch goal: publish to Kafka here
    }
}
