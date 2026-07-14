package com.example.orderfulfilment.service.worker;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateInvoiceWorker")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class GenerateInvoiceWorkerTest {

    @Mock
    private ExternalTask externalTask;

    @Mock
    private ExternalTaskService externalTaskService;

    @InjectMocks
    private GenerateInvoiceWorker worker;

    private static final String ORDER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        when(externalTask.getVariable("orderId")).thenReturn(ORDER_ID);
        when(externalTask.getVariable("customerId")).thenReturn("CUST-001");
        when(externalTask.getVariable("amount")).thenReturn(750.0);
    }

    @Test
    @DisplayName("successful execution should complete with invoice number and timestamp")
    void successfulExecution_generatesInvoice() {
        worker.execute(externalTask, externalTaskService);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(externalTaskService).complete(eq(externalTask), varsCaptor.capture());

        Map<String, Object> vars = varsCaptor.getValue();
        assertThat((String) vars.get("invoiceNumber")).startsWith("INV-");
        assertThat(vars.get("invoiceGeneratedAt")).isNotNull();
    }

    @Test
    @DisplayName("topic name should be generate-invoice")
    void topicName_isCorrect() {
        assertThat(worker.getTopicName()).isEqualTo("generate-invoice");
    }

    @Test
    @DisplayName("handles null orderId gracefully by still completing")
    void nullOrderId_stillCompletes() {
        // GenerateInvoiceWorker uses orderId only in log statements and invoice ref.
        // Null orderId in logs is handled by SLF4J. Invoice uses System.currentTimeMillis().
        // Worker should still complete successfully.
        when(externalTask.getVariable("orderId")).thenReturn(null);
        when(externalTask.getVariable("customerId")).thenReturn(null);
        when(externalTask.getVariable("amount")).thenReturn(null);

        worker.execute(externalTask, externalTaskService);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(externalTaskService).complete(eq(externalTask), varsCaptor.capture());
        assertThat((String) varsCaptor.getValue().get("invoiceNumber")).startsWith("INV-");
    }
}
