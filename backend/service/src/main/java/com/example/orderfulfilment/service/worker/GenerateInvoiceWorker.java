package com.example.orderfulfilment.service.worker;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * External task worker for topic: generate-invoice
 *
 * Runs in parallel with ReserveInventoryWorker after credit is secured.
 * Simulates invoice generation — log-based implementation.
 */
@Component
@Slf4j
public class GenerateInvoiceWorker extends BaseExternalTaskWorker {

    @Override
    public String getTopicName() {
        return "generate-invoice";
    }

    @Override
    protected Map<String, Object> executeTask(ExternalTask task) {
        String orderId      = task.getVariable("orderId");
        String customerId   = task.getVariable("customerId");
        Double amount       = task.getVariable("amount");

        String invoiceNumber = "INV-" + System.currentTimeMillis();

        log.info("[GenerateInvoice] Generating invoice {} for orderId={} customerId={} amount={}",
                invoiceNumber, orderId, customerId, amount);

        // Simulated generation — in production this would call a billing service
        log.info("[GenerateInvoice] Invoice {} generated at {} for orderId={}",
                invoiceNumber, Instant.now(), orderId);

        return Map.of(
                "invoiceNumber",    invoiceNumber,
                "invoiceGeneratedAt", Instant.now().toString()
        );
    }
}
