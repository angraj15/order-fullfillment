package com.example.orderfulfilment.service.exception;

import com.example.orderfulfilment.service.entity.OrderStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Invalid order status transition: " + from + " -> " + to);
    }
}
