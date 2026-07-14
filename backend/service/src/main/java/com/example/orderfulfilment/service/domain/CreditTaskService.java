package com.example.orderfulfilment.service.domain;

import com.example.orderfulfilment.service.client.CamundaRestClient;
import com.example.orderfulfilment.service.client.dto.CamundaTaskDto;
import com.example.orderfulfilment.service.entity.Order;
import com.example.orderfulfilment.service.entity.OrderStatus;
import com.example.orderfulfilment.service.exception.OrderNotFoundException;
import com.example.orderfulfilment.service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for credit override user task management.
 *
 * Fetches pending user tasks from the Camunda engine and enriches them
 * with local order data. Completes tasks with approve/reject decisions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditTaskService {

    private final CamundaRestClient camundaRestClient;
    private final OrderRepository   orderRepository;
    private final OrderService      orderService;

    /**
     * Fetch all pending credit override tasks from Camunda and
     * enrich each with the corresponding local Order.
     */
    @Transactional(readOnly = true)
    public List<CreditTaskView> getPendingTasks() {
        List<CamundaTaskDto> tasks = camundaRestClient.getPendingCreditOverrideTasks();
        log.debug("Found {} pending credit override tasks", tasks.size());

        return tasks.stream()
                .map(task -> {
                    Order order = orderRepository
                            .findByProcessInstanceId(task.getProcessInstanceId())
                            .orElseThrow(() -> new OrderNotFoundException(task.getProcessInstanceId()));
                    return new CreditTaskView(task, order);
                })
                .toList();
    }

    /**
     * Complete a credit override user task with an approve/reject decision.
     * Updates local order status accordingly.
     */
    @Transactional
    public void completeTask(String taskId, String processInstanceId,
                             boolean approved, String comment) {
        log.info("Completing credit task '{}' — approved={}, comment='{}'",
                taskId, approved, comment);

        // Complete the task in Camunda
        camundaRestClient.completeTask(taskId, Map.of(
                "approved",       approved,
                "decisionComment", comment != null ? comment : ""
        ));

        // Update local order status
        Order order = orderRepository.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new OrderNotFoundException(processInstanceId));

        OrderStatus newStatus = approved ? OrderStatus.APPROVED : OrderStatus.REJECTED;
        String reason = approved
                ? "Credit approved by officer: " + comment
                : "Credit rejected by officer: " + comment;

        orderService.updateStatus(order.getId(), newStatus, reason);
    }

    /**
     * Read-only view combining Camunda task data with local order data.
     */
    public record CreditTaskView(CamundaTaskDto task, Order order) {}
}
