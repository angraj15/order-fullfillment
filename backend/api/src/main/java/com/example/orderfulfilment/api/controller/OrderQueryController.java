package com.example.orderfulfilment.api.controller;

import com.example.orderfulfilment.api.dto.OrderResponse;
import com.example.orderfulfilment.service.domain.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Read-only order query endpoints.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order submission and query operations")
public class OrderQueryController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List all orders", description = "Returns all orders sorted by creation date descending.")
    public List<OrderResponse> listOrders() {
        return orderService.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return OrderResponse.from(orderService.findById(id));
    }
}
