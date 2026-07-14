package com.example.orderfulfilment.service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a customer order.
 * Uses @Version for optimistic locking — prevents lost updates when
 * parallel external task workers update the same order concurrently.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_status",           columnList = "status"),
    @Index(name = "idx_orders_process_instance", columnList = "process_instance_id"),
    @Index(name = "idx_orders_customer_id",      columnList = "customer_id")
})
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Order {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_pref", nullable = false, length = 10)
    private NotificationPreference notificationPreference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    /**
     * Optimistic locking version — JPA increments on every UPDATE.
     * Prevents two threads clobbering each other's status update.
     */
    @Version
    private Long version;

    @Column(name = "process_instance_id", length = 100)
    private String processInstanceId;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
