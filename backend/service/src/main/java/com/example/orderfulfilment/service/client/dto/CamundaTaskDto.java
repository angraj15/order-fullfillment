package com.example.orderfulfilment.service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Minimal projection of Camunda's Task REST response.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamundaTaskDto {
    private String id;
    private String name;
    private String processInstanceId;
    private String processDefinitionId;
    private String taskDefinitionKey;
    private OffsetDateTime created;
}
