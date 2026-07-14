package com.example.orderfulfilment.service.client;

import com.example.orderfulfilment.service.client.dto.CamundaTaskDto;
import com.example.orderfulfilment.service.client.dto.CamundaVariableDto;
import com.example.orderfulfilment.service.client.dto.ProcessInstanceDto;
import com.example.orderfulfilment.service.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter over the Camunda 7 REST API.
 *
 * Design patterns applied:
 *  - Adapter: translates Camunda HTTP API into a typed Java interface.
 *  - Circuit Breaker (Resilience4j): prevents cascading failures when the engine is down.
 *  - Retry: transparently retries transient HTTP errors with exponential backoff.
 *
 * All methods that modify engine state are protected by @Retry(name = "camunda").
 * startProcess() additionally has a @CircuitBreaker with a fallback that keeps
 * the order in RECEIVED state and returns a 503 to the caller.
 */
@Component
@Slf4j
public class CamundaRestClient {

    private static final String CB_NAME = "camunda";
    private static final String CREDIT_OVERRIDE_TASK_KEY = "task_approve_credit_override";

    private final RestClient restClient;
    private final String baseUrl;

    public CamundaRestClient(RestClient.Builder builder,
                             @Value("${camunda.client.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // -------------------------------------------------------------------------
    // Process Instance
    // -------------------------------------------------------------------------

    /**
     * Start a new process instance for the given process definition key.
     * Protected by circuit breaker — falls back gracefully when engine is unreachable.
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "startProcessFallback")
    @Retry(name = CB_NAME)
    public ProcessInstanceDto startProcess(String processKey, Map<String, Object> variables) {
        log.info("Starting process '{}' with {} variables", processKey, variables.size());

        Map<String, CamundaVariableDto> camundaVars = toVariableMap(variables);
        Map<String, Object> body = Map.of("variables", camundaVars);

        return restClient.post()
                .uri("/process-definition/key/{key}/start", processKey)
                .body(body)
                .retrieve()
                .body(ProcessInstanceDto.class);
    }

    @SuppressWarnings("unused") // called by Resilience4j via reflection
    private ProcessInstanceDto startProcessFallback(String processKey,
                                                     Map<String, Object> variables,
                                                     Exception ex) {
        log.error("Circuit open — Camunda engine unavailable when starting process '{}': {}",
                processKey, ex.getMessage());
        throw new ServiceUnavailableException(
                "Process engine is currently unavailable. Order saved, will retry.", ex);
    }

    // -------------------------------------------------------------------------
    // Message Correlation
    // -------------------------------------------------------------------------

    @Retry(name = CB_NAME)
    public void correlateMessage(String messageName, String processInstanceId) {
        log.info("Correlating message '{}' to processInstanceId={}", messageName, processInstanceId);

        Map<String, Object> body = Map.of(
                "messageName", messageName,
                "processInstanceId", processInstanceId
        );

        restClient.post()
                .uri("/message")
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    // -------------------------------------------------------------------------
    // User Tasks
    // -------------------------------------------------------------------------

    @Retry(name = CB_NAME)
    public List<CamundaTaskDto> getPendingCreditOverrideTasks() {
        log.debug("Fetching pending credit override tasks");

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/task")
                        .queryParam("taskDefinitionKey", CREDIT_OVERRIDE_TASK_KEY)
                        .queryParam("active", "true")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<CamundaTaskDto>>() {});
    }

    @Retry(name = CB_NAME)
    public void completeTask(String taskId, Map<String, Object> variables) {
        log.info("Completing task '{}'", taskId);

        Map<String, CamundaVariableDto> camundaVars = toVariableMap(variables);
        Map<String, Object> body = Map.of("variables", camundaVars);

        restClient.post()
                .uri("/task/{id}/complete", taskId)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    // -------------------------------------------------------------------------
    // Deployment
    // -------------------------------------------------------------------------

    /**
     * Check whether a process with the given key is already deployed.
     * Returns false on 404 (not deployed), throws on other errors.
     */
    public boolean isProcessDeployed(String processKey) {
        try {
            restClient.get()
                    .uri("/process-definition/key/{key}", processKey)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    /**
     * Deploy a BPMN file from the classpath to the Camunda engine.
     * Uses multipart/form-data as required by the Camunda deployment API.
     * Uses RestTemplate for multipart support (RestClient multipart is unreliable).
     */
    public void deployProcess(String classpathResource, String deploymentName) {
        log.info("Deploying BPMN '{}' as '{}'", classpathResource, deploymentName);

        try {
            ClassPathResource resource = new ClassPathResource(classpathResource);

            org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("deployment-name", deploymentName);
            body.add("deploy-changed-only", "true");
            body.add("data", resource);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity =
                    new org.springframework.http.HttpEntity<>(body, headers);

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            restTemplate.postForEntity(baseUrl + "/deployment/create", requestEntity, String.class);

            log.info("BPMN deployed successfully: {}", classpathResource);
        } catch (Exception ex) {
            throw new ServiceUnavailableException("Failed to deploy BPMN process: " + ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, CamundaVariableDto> toVariableMap(Map<String, Object> variables) {
        Map<String, CamundaVariableDto> result = new HashMap<>();
        variables.forEach((k, v) -> result.put(k, CamundaVariableDto.of(v)));
        return result;
    }
}
