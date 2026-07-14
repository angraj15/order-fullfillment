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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidateOrderWorker")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ValidateOrderWorkerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ExternalTask externalTask;

    @Mock
    private ExternalTaskService externalTaskService;

    @InjectMocks
    private ValidateOrderWorker worker;

    private static final String ORDER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        when(externalTask.getVariable("orderId")).thenReturn(ORDER_ID);
        when(orderService.updateStatus(any(), any(), any()))
                .thenReturn(Order.builder().id(UUID.fromString(ORDER_ID)).status(OrderStatus.VALIDATING).build());
    }

    @Test
    @DisplayName("valid payload should return VALID result and update status to VALIDATING")
    void validPayload_returnsValid() {
        when(externalTask.getVariable("customerId")).thenReturn("CUST-001");
        when(externalTask.getVariable("customerName")).thenReturn("Alice Smith");
        when(externalTask.getVariable("amount")).thenReturn(500.0);

        worker.execute(externalTask, externalTaskService);

        // Verify task completed with correct variables
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(externalTaskService).complete(eq(externalTask), varsCaptor.capture());

        Map<String, Object> vars = varsCaptor.getValue();
        assertThat(vars.get("validationResult")).isEqualTo("VALID");
        assertThat(vars.get("validationReason")).isEqualTo("Order is valid");

        // Verify status updated to VALIDATING
        verify(orderService).updateStatus(UUID.fromString(ORDER_ID), OrderStatus.VALIDATING, "Order is valid");
    }

    @Test
    @DisplayName("null customerId should return INVALID")
    void nullCustomerId_returnsInvalid() {
        when(externalTask.getVariable("customerId")).thenReturn(null);
        when(externalTask.getVariable("customerName")).thenReturn("Alice");
        when(externalTask.getVariable("amount")).thenReturn(100.0);
        when(orderService.updateStatus(any(), any(), any()))
                .thenReturn(Order.builder().id(UUID.fromString(ORDER_ID)).status(OrderStatus.REJECTED).build());

        worker.execute(externalTask, externalTaskService);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(externalTaskService).complete(eq(externalTask), varsCaptor.capture());

        assertThat(varsCaptor.getValue().get("validationResult")).isEqualTo("INVALID");
        assertThat(varsCaptor.getValue().get("validationReason")).isEqualTo("customerId is required");
        verify(orderService).updateStatus(UUID.fromString(ORDER_ID), OrderStatus.REJECTED, "customerId is required");
    }

    @Test
    @DisplayName("blank customerName should return INVALID")
    void blankCustomerName_returnsInvalid() {
        when(externalTask.getVariable("customerId")).thenReturn("CUST-001");
        when(externalTask.getVariable("customerName")).thenReturn("  ");
        when(externalTask.getVariable("amount")).thenReturn(100.0);
        when(orderService.updateStatus(any(), any(), any()))
                .thenReturn(Order.builder().id(UUID.fromString(ORDER_ID)).status(OrderStatus.REJECTED).build());

        worker.execute(externalTask, externalTaskService);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(externalTaskService).complete(eq(externalTask), varsCaptor.capture());

        assertThat(varsCaptor.getValue().get("validationResult")).isEqualTo("INVALID");
        assertThat(varsCaptor.getValue().get("validationReason")).isEqualTo("customerName is required");
    }

    @Test
    @DisplayName("negative amount should return INVALID")
    void negativeAmount_returnsInvalid() {
        when(externalTask.getVariable("customerId")).thenReturn("CUST-001");
        when(externalTask.getVariable("customerName")).thenReturn("Alice");
        when(externalTask.getVariable("amount")).thenReturn(-50.0);
        when(orderService.updateStatus(any(), any(), any()))
                .thenReturn(Order.builder().id(UUID.fromString(ORDER_ID)).status(OrderStatus.REJECTED).build());

        worker.execute(externalTask, externalTaskService);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(externalTaskService).complete(eq(externalTask), varsCaptor.capture());

        assertThat(varsCaptor.getValue().get("validationResult")).isEqualTo("INVALID");
        assertThat(varsCaptor.getValue().get("validationReason")).isEqualTo("amount must be a positive value");
    }

    @Test
    @DisplayName("zero amount should return INVALID")
    void zeroAmount_returnsInvalid() {
        when(externalTask.getVariable("customerId")).thenReturn("CUST-001");
        when(externalTask.getVariable("customerName")).thenReturn("Alice");
        when(externalTask.getVariable("amount")).thenReturn(0.0);
        when(orderService.updateStatus(any(), any(), any()))
                .thenReturn(Order.builder().id(UUID.fromString(ORDER_ID)).status(OrderStatus.REJECTED).build());

        worker.execute(externalTask, externalTaskService);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(externalTaskService).complete(eq(externalTask), varsCaptor.capture());

        assertThat(varsCaptor.getValue().get("validationResult")).isEqualTo("INVALID");
    }

    @Test
    @DisplayName("validate() method — unit test of pure logic without mocks")
    void validateMethod_directTest() {
        assertThat(worker.validate("CUST-1", "Alice", 100.0).isValid()).isTrue();
        assertThat(worker.validate(null, "Alice", 100.0).isValid()).isFalse();
        assertThat(worker.validate("", "Alice", 100.0).isValid()).isFalse();
        assertThat(worker.validate("CUST-1", null, 100.0).isValid()).isFalse();
        assertThat(worker.validate("CUST-1", "Alice", null).isValid()).isFalse();
        assertThat(worker.validate("CUST-1", "Alice", -1.0).isValid()).isFalse();
    }
}
