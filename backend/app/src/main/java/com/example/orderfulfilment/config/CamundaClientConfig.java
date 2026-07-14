package com.example.orderfulfilment.config;

import com.example.orderfulfilment.service.worker.*;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures and starts the Camunda External Task Client.
 *
 * Registers all 6 external task workers as topic subscribers.
 * Uses long-polling (asyncResponseTimeout) to reduce polling overhead.
 * On shutdown (@PreDestroy), releases held task locks back to the engine
 * before JVM exit — prevents tasks from being stuck until lock expiry.
 *
 * Workers run on Java 21 virtual threads (spring.threads.virtual.enabled=true).
 * Each topic subscription is independent — workers execute concurrently.
 */
@Configuration
@Slf4j
public class CamundaClientConfig {

    private final ExternalTaskClient client;

    public CamundaClientConfig(
            @Value("${camunda.client.base-url}")           String baseUrl,
            @Value("${camunda.client.worker-id}")          String workerId,
            @Value("${camunda.client.lock-duration}")      long lockDuration,
            @Value("${camunda.client.async-response-timeout}") long asyncResponseTimeout,
            @Value("${camunda.client.max-tasks}")          int maxTasks,
            // All workers injected — Spring collects all BaseExternalTaskWorker beans
            List<BaseExternalTaskWorker> workers) {

        log.info("Initialising Camunda External Task Client — baseUrl={} workerId={}", baseUrl, workerId);

        this.client = ExternalTaskClient.create()
                .baseUrl(baseUrl)
                .workerId(workerId)
                .lockDuration(lockDuration)
                .asyncResponseTimeout(asyncResponseTimeout)
                .maxTasks(maxTasks)
                .build();

        // Register each worker as a topic subscriber
        for (BaseExternalTaskWorker worker : workers) {
            client.subscribe(worker.getTopicName())
                  .lockDuration(lockDuration)
                  .handler(worker)
                  .open();
            log.info("Subscribed worker '{}' to topic '{}'",
                    worker.getClass().getSimpleName(), worker.getTopicName());
        }

        log.info("External Task Client started with {} worker subscriptions", workers.size());
    }

    /**
     * Gracefully shut down the client before JVM exit.
     * Releases held task locks so the engine doesn't wait for lock expiry.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Camunda External Task Client — releasing held locks");
        try {
            client.stop();
            log.info("Camunda External Task Client stopped cleanly");
        } catch (Exception ex) {
            log.warn("Error stopping External Task Client: {}", ex.getMessage());
        }
    }
}
