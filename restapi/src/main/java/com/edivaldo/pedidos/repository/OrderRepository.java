package com.edivaldo.pedidos.repository;

import com.edivaldo.pedidos.enums.OrderStatus;
import com.edivaldo.pedidos.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByPartnerId(Long partnerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Order> findByPartnerIdAndStatus(Long partnerId, OrderStatus status);

    List<Order> findByPartnerIdAndCreatedAtBetween(Long partnerId, LocalDateTime startDate, LocalDateTime endDate);
}
