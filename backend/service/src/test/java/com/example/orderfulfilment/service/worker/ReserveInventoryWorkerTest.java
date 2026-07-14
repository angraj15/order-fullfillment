package com.example.orderfulfilment.service.worker;

import com.example.orderfulfilment.service.domain.OrderService;
import com.example.orderfulfilment.service.entity.Order;
import com.example.orderfulfilment.service.entity.OrderStatus;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReserveInventoryWorker")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ReserveInventoryWorkerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ExternalTask externalTask;

    @Mock
    private ExternalTaskService externalTaskService;

    @InjectMocks
    private ReserveInventoryWorker worker;

    private static final String ORDER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        when(externalTask.getVariable("orderId")).thenReturn(ORDER_ID);
        when(externalTask.getVariable("customerId")).thenReturn("CUST-001");
        when(externalTask.getVariable("amount")).thenReturn(2000.0);
    }

    @Test
    @DisplayName("successful execution should complete task with reservation reference")
    void successfulExecution_completesWithRef() {
        Order order = Order.builder().id(UUID.fromString(ORDER_ID)).status(OrderStatus.AUTO_APPROVED).build();
        when(orderService.findById(UUID.fromString(ORDER_ID))).thenReturn(order);

        worker.execute(externalTask, externalTaskService);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(externalTaskService).complete(eq(externalTask), varsCaptor.capture());

        Map<String, Object> vars = varsCaptor.getValue();
        assertThat(vars.get("inventoryReserved")).isEqualTo(true);
        assertThat((String) vars.get("inventoryReservationRef")).startsWith("INV-");
    }

    @Test
    @DisplayName("topic name should be reserve-inventory")
    void topicName_isCorrect() {
        assertThat(worker.getTopicName()).isEqualTo("reserve-inventory");
    }

    @Test
    @DisplayName("should handle failure with retry when exception occurs")
    void exception_triggersHandleFailure() {
        when(externalTask.getVariable("orderId")).thenReturn(null); // This will cause NPE in substring
        when(externalTask.getRetries()).thenReturn(3);

        worker.execute(externalTask, externalTaskService);

        verify(externalTaskService).handleFailure(
                eq(externalTask),
                any(String.class),
                any(String.class),
                eq(2),      // retries decremented from 3 to 2
                eq(1000L)   // first backoff = 1s
        );
        verify(externalTaskService, never()).complete(any(), any());
    }

    @Test
    @DisplayName("second retry should have 2000ms backoff")
    void secondRetry_has2sBackoff() {
        when(externalTask.getVariable("orderId")).thenReturn(null);
        when(externalTask.getRetries()).thenReturn(2);

        worker.execute(externalTask, externalTaskService);

        verify(externalTaskService).handleFailure(
                eq(externalTask),
                any(String.class),
                any(String.class),
                eq(1),      // retries decremented from 2 to 1
                eq(2000L)   // second backoff = 2s
        );
    }
}
