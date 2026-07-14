package com.example.orderfulfilment.api.controller;

import com.example.orderfulfilment.service.domain.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoint to correlate the "CustomerUpdatedPriority" message to a running process instance.
 * Demonstrates the non-interrupting Message Boundary Event in the BPMN.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order submission and query operations")
public class PriorityController {

    private final OrderService orderService;

    @PostMapping("/{id}/priority")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Send 'Customer Updated Priority' message",
               description = "Correlates a CustomerUpdatedPriority message to the running process. " +
                             "Triggers the non-interrupting boundary event, executing the Update SLA Log " +
                             "service task without interrupting the credit override user task.")
    public void updatePriority(@PathVariable UUID id) {
        log.info("Correlating CustomerUpdatedPriority message for order {}", id);
        orderService.correlateCustomerPriorityMessage(id);
    }
}
