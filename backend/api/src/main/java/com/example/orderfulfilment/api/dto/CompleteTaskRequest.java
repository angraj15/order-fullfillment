package com.example.orderfulfilment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for completing a credit-override user task.
 */
@Schema(description = "Credit override task completion request")
public record CompleteTaskRequest(

    @NotNull(message = "approved is required")
    @Schema(description = "Approval decision — true to approve, false to reject", example = "true")
    Boolean approved,

    @Size(max = 500, message = "comment must not exceed 500 characters")
    @Schema(description = "Officer comment / reason", example = "Credit verified, approved.")
    String comment
) {}
