package com.example.orderfulfilment.service.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event published when an order reaches FULFILLED status.
 * Listeners (audit, Kafka publisher, etc.) can react without coupling to OrderService.
 * Pattern: Observer (Spring ApplicationEvent)
 */
@Getter
public class OrderFulfilledEvent extends ApplicationEvent {

    private final UUID orderId;
    private final String customerId;
    private final String customerName;
    private final BigDecimal amount;

    public OrderFulfilledEvent(Object source, UUID orderId, String customerId,
                               String customerName, BigDecimal amount) {
        super(source);
        this.orderId      = orderId;
        this.customerId   = customerId;
        this.customerName = customerName;
        this.amount       = amount;
    }
}
