package com.example.orderfulfilment.service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Minimal projection of Camunda's ProcessInstance REST response.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessInstanceDto {
    private String id;
    private String definitionId;
    private String businessKey;
}
