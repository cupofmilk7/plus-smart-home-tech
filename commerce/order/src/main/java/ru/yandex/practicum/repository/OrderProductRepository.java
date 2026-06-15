package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.OrderProduct;

import java.util.List;
import java.util.UUID;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
    List<OrderProduct> findByOrderId(UUID orderId);
}