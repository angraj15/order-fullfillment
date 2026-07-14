package com.example.orderfulfilment.service.deployment;

import com.example.orderfulfilment.service.client.CamundaRestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Deploys the order-fulfilment BPMN process to the standalone Camunda engine on startup.
 *
 * Runs after the Spring context is fully initialised (ApplicationRunner).
 * Operation is idempotent: if the process is already deployed, it is skipped.
 * This is safe for rolling restarts and repeated startups.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BpmnDeploymentService implements ApplicationRunner {

    private static final String PROCESS_KEY      = "order-fulfilment";
    private static final String BPMN_CLASSPATH   = "processes/order-fulfilment.bpmn";
    private static final String DEPLOYMENT_NAME  = "Order Fulfilment Process";

    private final CamundaRestClient camundaRestClient;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (camundaRestClient.isProcessDeployed(PROCESS_KEY)) {
                log.info("BPMN process '{}' already deployed — skipping deployment.", PROCESS_KEY);
            } else {
                log.info("BPMN process '{}' not found — deploying from classpath '{}'",
                        PROCESS_KEY, BPMN_CLASSPATH);
                camundaRestClient.deployProcess(BPMN_CLASSPATH, DEPLOYMENT_NAME);
                log.info("BPMN process '{}' deployed successfully.", PROCESS_KEY);
            }
        } catch (Exception ex) {
            // Log but don't crash startup — engine may still be warming up.
            // Workers will fail gracefully until engine is ready.
            log.error("Failed to deploy BPMN process '{}': {}. " +
                      "Ensure Camunda engine is reachable at the configured URL.",
                      PROCESS_KEY, ex.getMessage());
        }
    }
}
