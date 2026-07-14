package com.example.orderfulfilment.service.domain;

import com.example.orderfulfilment.service.client.CamundaRestClient;
import com.example.orderfulfilment.service.client.dto.ProcessInstanceDto;
import com.example.orderfulfilment.service.entity.NotificationPreference;
import com.example.orderfulfilment.service.entity.Order;
import com.example.orderfulfilment.service.entity.OrderStatus;
import com.example.orderfulfilment.service.event.OrderFulfilledEvent;
import com.example.orderfulfilment.service.exception.InvalidStatusTransitionException;
import com.example.orderfulfilment.service.exception.OrderNotFoundException;
import com.example.orderfulfilment.service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Core domain service for order lifecycle management.
 *
 * Design patterns:
 *  - Facade: single entry point over JPA + Camunda + event publishing
 *  - State Machine: updateStatus() enforces valid transitions
 *  - Factory Method: buildOrder() creates Order from raw parameters
 *  - Observer: publishes OrderFulfilledEvent on terminal FULFILLED transition
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String PROCESS_KEY = "order-fulfilment";

    /**
     * Valid state transitions. Any transition NOT in this map is rejected.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
        OrderStatus.RECEIVED,         Set.of(OrderStatus.VALIDATING, OrderStatus.PENDING_OVERRIDE, OrderStatus.REJECTED),
        OrderStatus.VALIDATING,       Set.of(OrderStatus.AUTO_APPROVED, OrderStatus.PENDING_OVERRIDE, OrderStatus.REJECTED),
        OrderStatus.AUTO_APPROVED,    Set.of(OrderStatus.FULFILLED),
        OrderStatus.PENDING_OVERRIDE, Set.of(OrderStatus.APPROVED, OrderStatus.REJECTED, OrderStatus.AUTO_CANCELLED),
        OrderStatus.APPROVED,         Set.of(OrderStatus.FULFILLED),
        OrderStatus.REJECTED,         Set.of(),
        OrderStatus.AUTO_CANCELLED,   Set.of(),
        OrderStatus.FULFILLED,        Set.of()
    );

    private final OrderRepository          orderRepository;
    private final CamundaRestClient        camundaRestClient;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${order.timer-duration:PT10M}")
    private String timerDuration;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    /**
     * Persist a new order and start its Camunda process instance.
     * Returns the saved order with the assigned processInstanceId.
     */
    @Transactional
    public Order createOrder(String customerId, String customerName,
                             BigDecimal amount, NotificationPreference notificationPreference) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .customerName(customerName)
                .amount(amount)
                .notificationPreference(notificationPreference)
                .status(OrderStatus.RECEIVED)
                .build();

        order = orderRepository.save(order);
        log.info("Order {} created with status RECEIVED", order.getId());

        // Start Camunda process — circuit breaker handles engine unavailability
        Map<String, Object> variables = buildProcessVariables(order);
        ProcessInstanceDto instance = camundaRestClient.startProcess(PROCESS_KEY, variables);

        order.setProcessInstanceId(instance.getId());
        order = orderRepository.save(order);
        log.info("Process instance {} started for order {}", instance.getId(), order.getId());

        return order;
    }

    // -------------------------------------------------------------------------
    // Status Transitions
    // -------------------------------------------------------------------------

    /**
     * Update order status with transition validation and optimistic lock retry.
     * Publishes OrderFulfilledEvent on terminal FULFILLED transition.
     */
    @Transactional
    public Order updateStatus(UUID orderId, OrderStatus newStatus, String reason) {
        Order order = findById(orderId);
        validateTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        if (reason != null && !reason.isBlank()) {
            order.setDecisionReason(reason);
        }

        try {
            order = orderRepository.save(order);
            log.info("Order {} status: {} -> {}", orderId, order.getStatus(), newStatus);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic lock conflict on order {} — reloading and retrying", orderId);
            // Reload and retry once — concurrent worker already updated status
            order = findById(orderId);
            if (order.getStatus() != newStatus) {
                validateTransition(order.getStatus(), newStatus);
                order.setStatus(newStatus);
                if (reason != null && !reason.isBlank()) {
                    order.setDecisionReason(reason);
                }
                order = orderRepository.save(order);
            }
        }

        if (newStatus == OrderStatus.FULFILLED) {
            eventPublisher.publishEvent(new OrderFulfilledEvent(
                    this, order.getId(), order.getCustomerId(),
                    order.getCustomerName(), order.getAmount()));
            log.info("OrderFulfilledEvent published for order {}", orderId);
        }

        return order;
    }

    /**
     * Update order status by Camunda process instance ID.
     * Used by external task workers that only have the processInstanceId.
     */
    @Transactional
    public Order updateStatusByProcessInstance(String processInstanceId,
                                               OrderStatus newStatus, String reason) {
        Order order = orderRepository.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new OrderNotFoundException(processInstanceId));
        return updateStatus(order.getId(), newStatus, reason);
    }

    // -------------------------------------------------------------------------
    // Message Correlation
    // -------------------------------------------------------------------------

    public void correlateCustomerPriorityMessage(UUID orderId) {
        Order order = findById(orderId);
        if (order.getProcessInstanceId() == null) {
            throw new IllegalStateException("Order " + orderId + " has no running process instance");
        }
        log.info("Correlating CustomerUpdatedPriority message for order {} (processInstance={})",
                orderId, order.getProcessInstanceId());
        camundaRestClient.correlateMessage(
                "CustomerUpdatedPriority", order.getProcessInstanceId());
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Order findById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> buildProcessVariables(Order order) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderId",                 order.getId().toString());
        vars.put("customerId",              order.getCustomerId());
        vars.put("customerName",            order.getCustomerName());
        vars.put("amount",                  order.getAmount().doubleValue());
        vars.put("notificationPreference",  order.getNotificationPreference().name());
        vars.put("timerDuration",           timerDuration);
        return vars;
    }

    private void validateTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }
}
