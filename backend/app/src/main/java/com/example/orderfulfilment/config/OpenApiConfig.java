package com.example.orderfulfilment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration via springdoc-openapi.
 *
 * Accessible at:
 *   Swagger UI:  http://localhost:8080/swagger-ui.html
 *   API docs:    http://localhost:8080/v3/api-docs
 *
 * Basic Auth security scheme wired so Swagger UI can authenticate.
 */
@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH_SCHEME = "basicAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Order Fulfilment API")
                .version("1.0.0")
                .description("""
                    Order Fulfilment Workflow — Spring Boot · Camunda 7 (standalone) · Angular 22
                    
                    Processes customer orders through a BPMN workflow with:
                    - Credit decision (auto-approve ≤ €1,000 / manual override > €1,000)
                    - Parallel inventory reservation and invoice generation
                    - Multi-channel notification (EMAIL / SMS / BOTH)
                    
                    **Demo credentials:**
                    - customer / customer123  (CUSTOMER role)
                    - officer / officer123    (CREDIT_OFFICER role)
                    - admin / admin123        (both roles)
                    """)
                .contact(new Contact()
                    .name("Order Fulfilment Team")
                    .email("dev@example.com")))
            .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME))
            .components(new Components()
                .addSecuritySchemes(BASIC_AUTH_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("Use customer/customer123 or officer/officer123")));
    }
}
