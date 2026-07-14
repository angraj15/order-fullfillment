package com.example.orderfulfilment.api.controller;

import com.example.orderfulfilment.api.dto.OrderRequest;
import com.example.orderfulfilment.api.dto.OrderResponse;
import com.example.orderfulfilment.service.domain.OrderService;
import com.example.orderfulfilment.service.entity.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook endpoint that simulates an order arriving from an external sales channel.
 * Persists the order and starts its Camunda process instance.
 */

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order submission and query operations")
public class OrderWebhookController {

    private final OrderService orderService;

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a new order (webhook simulation)",
               description = "Persists the order and starts a Camunda process instance.")
    public OrderResponse receiveOrder(@Valid @RequestBody OrderRequest request) {
        log.info("Webhook received — customerId={} amount={}", request.customerId(), request.amount());

        Order order = orderService.createOrder(
                request.customerId(),
                request.customerName(),
                request.amount(),
                request.notificationPreference()
        );

        return OrderResponse.from(order);
    }
}
