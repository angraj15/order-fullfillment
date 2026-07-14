package com.example.orderfulfilment.service.entity;

/**
 * Lifecycle states for an order.
 * Transitions are enforced in OrderService.updateStatus().
 *
 * RECEIVED → VALIDATING → AUTO_APPROVED → FULFILLED
 *                       → PENDING_OVERRIDE → APPROVED  → FULFILLED
 *                                          → REJECTED
 *                                          → AUTO_CANCELLED
 *                       → REJECTED
 */
public enum OrderStatus {
    RECEIVED,
    VALIDATING,
    AUTO_APPROVED,
    PENDING_OVERRIDE,
    APPROVED,
    REJECTED,
    AUTO_CANCELLED,
    FULFILLED
}
