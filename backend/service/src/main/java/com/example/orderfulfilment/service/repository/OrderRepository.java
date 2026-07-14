package com.example.orderfulfilment.service.repository;

import com.example.orderfulfilment.service.entity.Order;
import com.example.orderfulfilment.service.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByProcessInstanceId(String processInstanceId);
}
