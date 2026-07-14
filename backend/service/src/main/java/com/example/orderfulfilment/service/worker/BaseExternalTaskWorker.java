package com.example.orderfulfilment.service.worker;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * Abstract base for all external task workers.
 *
 * Design pattern: Template Method
 *   execute() defines the fixed algorithm skeleton:
 *     1. Log start
 *     2. Delegate to executeTask() (subclass hook)
 *     3. Complete the task OR handle failure with exponential backoff
 *
 * Subclasses only implement getTopicName() and executeTask().
 * Error handling, logging, retry backoff are fully inherited.
 *
 * Retry strategy (exponential backoff):
 *   Attempt 1 fails -> retries=2, backoff=1000ms
 *   Attempt 2 fails -> retries=1, backoff=2000ms
 *   Attempt 3 fails -> retries=0, backoff=4000ms (task visible in Camunda Cockpit)
 */
@Slf4j
public abstract class BaseExternalTaskWorker implements ExternalTaskHandler {

    private static final int DEFAULT_RETRIES = 3;

    @Override
    public final void execute(ExternalTask task, ExternalTaskService service) {
        String orderId = task.getVariable("orderId");
        log.info("[Worker={}] START orderId={} taskId={}",
                getTopicName(), orderId, task.getId());

        try {
            Map<String, Object> outputVariables = executeTask(task);
            service.complete(task, outputVariables);
            log.info("[Worker={}] COMPLETE orderId={}", getTopicName(), orderId);

        } catch (Exception ex) {
            int retriesLeft = computeRetriesLeft(task);
            long retryTimeout = computeBackoffMs(retriesLeft);

            log.error("[Worker={}] FAILED orderId={} retries_remaining={} backoff_ms={}: {}",
                    getTopicName(), orderId, retriesLeft, retryTimeout, ex.getMessage(), ex);

            service.handleFailure(
                    task,
                    ex.getMessage(),
                    stackTrace(ex),
                    retriesLeft,
                    retryTimeout
            );
        }
    }

    /**
     * Execute the worker's business logic.
     * @return map of process variables to set on completion (may be empty)
     */
    protected abstract Map<String, Object> executeTask(ExternalTask task);

    /**
     * The Camunda external task topic this worker subscribes to.
     */
    public abstract String getTopicName();

    // -------------------------------------------------------------------------
    // Retry / backoff helpers
    // -------------------------------------------------------------------------

    private int computeRetriesLeft(ExternalTask task) {
        Integer current = task.getRetries();
        // First attempt: retries is null -> start at DEFAULT_RETRIES - 1
        // Subsequent: decrement by 1
        return (current == null) ? DEFAULT_RETRIES - 1 : Math.max(0, current - 1);
    }

    private long computeBackoffMs(int retriesLeft) {
        // retriesLeft: 2->1000ms, 1->2000ms, 0->4000ms
        int attempt = DEFAULT_RETRIES - 1 - retriesLeft; // 0, 1, 2
        return (long) Math.pow(2, attempt) * 1000L;
    }

    private String stackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
