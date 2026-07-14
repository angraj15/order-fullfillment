package com.example.orderfulfilment.api.controller;

import com.example.orderfulfilment.api.dto.CompleteTaskRequest;
import com.example.orderfulfilment.api.dto.TaskResponse;
import com.example.orderfulfilment.service.domain.CreditTaskService;
import com.example.orderfulfilment.service.domain.CreditTaskService.CreditTaskView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Credit-officer task management endpoints.
 * Lists and completes pending credit-override user tasks.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Credit Override Tasks", description = "Credit officer review operations")
public class TaskController {

    private final CreditTaskService creditTaskService;

    @GetMapping("/credit-override")
    @Operation(summary = "List pending credit override tasks",
               description = "Returns all active credit-override user tasks enriched with order data.")
    public List<TaskResponse> listPendingTasks() {
        return creditTaskService.getPendingTasks().stream()
                .map(TaskResponse::from)
                .toList();
    }

    @PostMapping("/credit-override/{taskId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Complete a credit override task",
               description = "Approve or reject the credit override. Rejection ends the process.")
    public void completeTask(
            @PathVariable String taskId,
            @Valid @RequestBody CompleteTaskRequest request) {

        log.info("Completing credit override task {} — approved={}", taskId, request.approved());

        // Fetch processInstanceId from the task view before completing
        CreditTaskView view = creditTaskService.getPendingTasks().stream()
                .filter(t -> t.task().getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        creditTaskService.completeTask(
                taskId,
                view.task().getProcessInstanceId(),
                request.approved(),
                request.comment()
        );
    }
}
