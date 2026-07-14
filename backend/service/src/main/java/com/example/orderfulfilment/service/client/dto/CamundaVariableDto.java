package com.example.orderfulfilment.service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single Camunda process variable in the REST API format.
 * { "value": ..., "type": "String" }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CamundaVariableDto {
    private Object value;
    private String type;

    public static CamundaVariableDto of(Object value) {
        String type = "String";
        if (value instanceof Boolean)    type = "Boolean";
        else if (value instanceof Integer) type = "Integer";
        else if (value instanceof Long)  type = "Long";
        else if (value instanceof Double) type = "Double";
        return new CamundaVariableDto(value, type);
    }
}
