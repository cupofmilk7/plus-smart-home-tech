package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.feign.ShoppingCartClient;
import ru.yandex.practicum.feign.WarehouseClient;
import ru.yandex.practicum.model.mapper.OrderMapper;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.model.OrderProduct;
import ru.yandex.practicum.repository.OrderProductRepository;
import ru.yandex.practicum.repository.OrderRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final ShoppingCartClient shoppingCartClient;
    private final WarehouseClient warehouseClient;

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getClientOrders(String username) {
        log.debug("Getting orders for user: {}", username);
        validateUsername(username);

        List<Order> orders = orderRepository.findByUsername(username);

        return orders.stream()
                .map(order -> {
                    List<OrderProduct> products = orderProductRepository.findByOrderId(order.getOrderId());
                    return OrderMapper.toDto(order, products);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDto createNewOrder(String username, CreateNewOrderRequest request) {
        log.debug("Creating new order for user: {}", username);
        validateUsername(username);

        ResponseEntity<ShoppingCartDto> cartResponse = shoppingCartClient.getShoppingCart(username);
        ShoppingCartDto cart = cartResponse.getBody();

        if (cart == null || cart.getProducts().isEmpty()) {
            throw new IllegalArgumentException("Shopping cart is empty");
        }

        try {
            ResponseEntity<BookedProductsDto> checkResponse = warehouseClient.checkProductQuantityEnoughForShoppingCart(cart);
            BookedProductsDto booked = checkResponse.getBody();

            if (booked == null) {
                throw new NoSpecifiedProductInWarehouseException("Failed to check warehouse availability");
            }

            Order order = Order.builder()
                    .shoppingCartId(cart.getShoppingCartId())
                    .username(username)
                    .paymentId(UUID.randomUUID())
                    .deliveryId(UUID.randomUUID())
                    .state(OrderState.NEW)
                    .deliveryWeight(booked.getDeliveryWeight())
                    .deliveryVolume(booked.getDeliveryVolume())
                    .fragile(booked.getFragile())
                    .productPrice(calculateProductPrice(cart.getProducts()))
                    .deliveryPrice(calculateDeliveryPrice(booked))
                    .totalPrice(calculateProductPrice(cart.getProducts()) + calculateDeliveryPrice(booked))
                    .build();

            Order savedOrder = orderRepository.save(order);

            for (Map.Entry<UUID, Integer> entry : cart.getProducts().entrySet()) {
                OrderProduct orderProduct = OrderProduct.builder()
                        .orderId(savedOrder.getOrderId())
                        .productId(entry.getKey())
                        .quantity(entry.getValue())
                        .build();
                orderProductRepository.save(orderProduct);
            }

            shoppingCartClient.deactivateCurrentShoppingCart(username);

            log.info("Order created successfully: {}", savedOrder.getOrderId());

            List<OrderProduct> products = orderProductRepository.findByOrderId(savedOrder.getOrderId());
            return OrderMapper.toDto(savedOrder, products);

        } catch (Exception e) {
            log.error("Failed to create order: {}", e.getMessage());
            throw new NoSpecifiedProductInWarehouseException("Products not available in warehouse: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        log.debug("Returning products for order: {}", request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NoOrderFoundException("Order not found: " + request.getOrderId()));

        order.setState(OrderState.PRODUCT_RETURNED);
        Order updatedOrder = orderRepository.save(order);

        List<OrderProduct> products = orderProductRepository.findByOrderId(updatedOrder.getOrderId());
        return OrderMapper.toDto(updatedOrder, products);
    }

    @Override
    @Transactional
    public OrderDto payment(UUID orderId) {
        log.debug("Processing payment for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order not found: " + orderId));

        order.setState(OrderState.PAID);
        Order updatedOrder = orderRepository.save(order);

        List<OrderProduct> products = orderProductRepository.findByOrderId(updatedOrder.getOrderId());
        return OrderMapper.toDto(updatedOrder, products);
    }

    @Override
    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        log.debug("Payment failed for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order not found: " + orderId));

        order.setState(OrderState.PAYMENT_FAILED);
        Order updatedOrder = orderRepository.save(order);

        List<OrderProduct> products = orderProductRepository.findByOrderId(updatedOrder.getOrderId());
        return OrderMapper.toDto(updatedOrder, products);
    }

    @Override
    @Transactional
    public OrderDto delivery(UUID orderId) {
        log.debug("Processing delivery for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order not found: " + orderId));

        order.setState(OrderState.DELIVERED);
        Order updatedOrder = orderRepository.save(order);

        List<OrderProduct> products = orderProductRepository.findByOrderId(updatedOrder.getOrderId());
        return OrderMapper.toDto(updatedOrder, products);
    }

    @Override
    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        log.debug("Delivery failed for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order not found: " + orderId));

        order.setState(OrderState.DELIVERY_FAILED);
        Order updatedOrder = orderRepository.save(order);

        List<OrderProduct> products = orderProductRepository.findByOrderId(updatedOrder.getOrderId());
        return OrderMapper.toDto(updatedOrder, products);
    }

    @Override
    @Transactional
    public OrderDto assembly(UUID orderId) {
        log.debug("Processing assembly for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order not found: " + orderId));

        order.setState(OrderState.ASSEMBLED);
        Order updatedOrder = orderRepository.save(order);

        List<OrderProduct> products = orderProductRepository.findByOrderId(updatedOrder.getOrderId());
        return OrderMapper.toDto(updatedOrder, products);
    }

    @Override
    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        log.debug("Assembly failed for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order not found: " + orderId));

        order.setState(OrderState.ASSEMBLY_FAILED);
        Order updatedOrder = orderRepository.save(order);

        List<OrderProduct> products = orderProductRepository.findByOrderId(updatedOrder.getOrderId());
        return OrderMapper.toDto(updatedOrder, products);
    }

    @Override
    @Transactional
    public OrderDto complete(UUID orderId) {
        log.debug("Completing order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order not found: " + orderId));

        order.setState(OrderState.COMPLETED);
        Order updatedOrder = orderRepository.save(order);

        List<OrderProduct> products = orderProductRepository.findByOrderId(updatedOrder.getOrderId());
        return OrderMapper.toDto(updatedOrder, products);
    }

    @Override
    @Transactional
    public OrderDto calculateTotalCost(UUID orderId) {
        log.debug("Calculating total cost for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order not found: " + orderId));

        order.setTotalPrice(order.getProductPrice() + order.getDeliveryPrice());
        Order updatedOrder = orderRepository.save(order);

        List<OrderProduct> products = orderProductRepository.findByOrderId(updatedOrder.getOrderId());
        return OrderMapper.toDto(updatedOrder, products);
    }

    @Override
    @Transactional
    public OrderDto calculateDeliveryCost(UUID orderId) {
        log.debug("Calculating delivery cost for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order not found: " + orderId));

        BookedProductsDto booked = new BookedProductsDto(
                order.getDeliveryWeight(),
                order.getDeliveryVolume(),
                order.getFragile()
        );
        order.setDeliveryPrice(calculateDeliveryPrice(booked));
        order.setTotalPrice(order.getProductPrice() + order.getDeliveryPrice());
        Order updatedOrder = orderRepository.save(order);

        List<OrderProduct> products = orderProductRepository.findByOrderId(updatedOrder.getOrderId());
        return OrderMapper.toDto(updatedOrder, products);
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
    }

    private double calculateProductPrice(Map<UUID, Integer> products) {
        return products.values().stream().mapToDouble(Integer::doubleValue).sum() * 100.0;
    }

    private double calculateDeliveryPrice(BookedProductsDto booked) {
        double basePrice = 500.0;
        double weightCost = booked.getDeliveryWeight() * 100.0;
        double volumeCost = booked.getDeliveryVolume() * 50.0;
        double fragileCost = Boolean.TRUE.equals(booked.getFragile()) ? 200.0 : 0.0;
        return basePrice + weightCost + volumeCost + fragileCost;
    }
}