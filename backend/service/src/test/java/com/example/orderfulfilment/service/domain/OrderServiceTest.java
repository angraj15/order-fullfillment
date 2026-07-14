package com.example.orderfulfilment.service.domain;

import com.example.orderfulfilment.service.client.CamundaRestClient;
import com.example.orderfulfilment.service.client.dto.ProcessInstanceDto;
import com.example.orderfulfilment.service.entity.NotificationPreference;
import com.example.orderfulfilment.service.entity.Order;
import com.example.orderfulfilment.service.entity.OrderStatus;
import com.example.orderfulfilment.service.exception.InvalidStatusTransitionException;
import com.example.orderfulfilment.service.exception.OrderNotFoundException;
import com.example.orderfulfilment.service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CamundaRestClient camundaRestClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "timerDuration", "PT10M");

        testOrder = Order.builder()
                .id(orderId)
                .customerId("CUST-001")
                .customerName("Alice Smith")
                .amount(BigDecimal.valueOf(500))
                .notificationPreference(NotificationPreference.EMAIL)
                .status(OrderStatus.RECEIVED)
                .build();
    }

    @Test
    @DisplayName("createOrder should persist order and start Camunda process")
    void createOrder_persistsAndStartsProcess() {
        ProcessInstanceDto processInstance = new ProcessInstanceDto();
        processInstance.setId("proc-123");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(camundaRestClient.startProcess(eq("order-fulfilment"), any())).thenReturn(processInstance);

        Order result = orderService.createOrder("CUST-001", "Alice Smith",
                BigDecimal.valueOf(500), NotificationPreference.EMAIL);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(result.getProcessInstanceId()).isEqualTo("proc-123");
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(camundaRestClient).startProcess(eq("order-fulfilment"), any());
    }

    @Test
    @DisplayName("updateStatus with valid transition should succeed")
    void updateStatus_validTransition_succeeds() {
        testOrder.setStatus(OrderStatus.VALIDATING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.updateStatus(orderId, OrderStatus.PENDING_OVERRIDE, "Override required");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING_OVERRIDE);
        assertThat(result.getDecisionReason()).isEqualTo("Override required");
    }

    @Test
    @DisplayName("updateStatus to FULFILLED should publish OrderFulfilledEvent")
    void updateStatus_toFulfilled_publishesEvent() {
        testOrder.setStatus(OrderStatus.AUTO_APPROVED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateStatus(orderId, OrderStatus.FULFILLED, "Done");

        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("updateStatus with invalid transition should throw InvalidStatusTransitionException")
    void updateStatus_invalidTransition_throws() {
        testOrder.setStatus(OrderStatus.FULFILLED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.updateStatus(orderId, OrderStatus.RECEIVED, ""))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("FULFILLED")
                .hasMessageContaining("RECEIVED");
    }

    @Test
    @DisplayName("findById with non-existent ID should throw OrderNotFoundException")
    void findById_nonExistent_throws() {
        UUID randomId = UUID.randomUUID();
        when(orderRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(randomId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("amount <= 1000 should result in VALIDATING -> AUTO_APPROVED path")
    void smallOrder_autoApproved() {
        // After validation, small orders go VALIDATING → AUTO_APPROVED
        testOrder.setStatus(OrderStatus.VALIDATING);
        testOrder.setAmount(BigDecimal.valueOf(500));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.updateStatus(orderId, OrderStatus.AUTO_APPROVED, "Auto-approved: amount <= 1000");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.AUTO_APPROVED);
    }

    @Test
    @DisplayName("amount > 1000 should result in VALIDATING -> PENDING_OVERRIDE path")
    void largeOrder_pendingOverride() {
        testOrder.setStatus(OrderStatus.VALIDATING);
        testOrder.setAmount(BigDecimal.valueOf(5000));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.updateStatus(orderId, OrderStatus.PENDING_OVERRIDE, "Credit override required");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING_OVERRIDE);
    }
}
