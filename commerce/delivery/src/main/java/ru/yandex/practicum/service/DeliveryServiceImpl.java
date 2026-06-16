package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.feign.OrderClient;
import ru.yandex.practicum.feign.WarehouseClient;
import ru.yandex.practicum.model.Delivery;
import ru.yandex.practicum.repository.DeliveryRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderClient orderClient;
    private final WarehouseClient warehouseClient;

    private static final double BASE_COST = 5.0;

    @Override
    @Transactional
    public DeliveryDto planDelivery(DeliveryDto deliveryDto) {
        log.debug("Planning delivery for order: {}", deliveryDto.getOrderId());

        Delivery delivery = Delivery.builder()
                .deliveryId(UUID.randomUUID())
                .orderId(deliveryDto.getOrderId())
                .fromCountry(deliveryDto.getFromAddress().getCountry())
                .fromCity(deliveryDto.getFromAddress().getCity())
                .fromStreet(deliveryDto.getFromAddress().getStreet())
                .fromHouse(deliveryDto.getFromAddress().getHouse())
                .fromFlat(deliveryDto.getFromAddress().getFlat())
                .toCountry(deliveryDto.getToAddress().getCountry())
                .toCity(deliveryDto.getToAddress().getCity())
                .toStreet(deliveryDto.getToAddress().getStreet())
                .toHouse(deliveryDto.getToAddress().getHouse())
                .toFlat(deliveryDto.getToAddress().getFlat())
                .deliveryState(DeliveryState.CREATED)
                .build();

        Delivery saved = deliveryRepository.save(delivery);
        log.info("Delivery planned: {} for order: {}", saved.getDeliveryId(), saved.getOrderId());

        return DeliveryDto.builder()
                .deliveryId(saved.getDeliveryId())
                .fromAddress(deliveryDto.getFromAddress())
                .toAddress(deliveryDto.getToAddress())
                .orderId(saved.getOrderId())
                .deliveryState(saved.getDeliveryState())
                .build();
    }

    @Override
    public Double calculateDeliveryCost(OrderDto order) {
        log.debug("Calculating delivery cost for order: {}", order.getOrderId());

        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress().getBody();
        if (warehouseAddress == null) {
            throw new RuntimeException("Warehouse address not found");
        }

        double cost = BASE_COST;

        String warehouseStreet = warehouseAddress.getStreet();
        if ("ADDRESS_1".equals(warehouseStreet)) {
            cost = BASE_COST + (BASE_COST * 1);
        } else if ("ADDRESS_2".equals(warehouseStreet)) {
            cost = BASE_COST + (BASE_COST * 2);
        }

        if (order.getFragile() != null && order.getFragile()) {
            cost += cost * 0.2;
        }

        if (order.getDeliveryWeight() != null) {
            cost += order.getDeliveryWeight() * 0.3;
        }

        if (order.getDeliveryVolume() != null) {
            cost += order.getDeliveryVolume() * 0.2;
        }

        if (order.getDeliveryAddress() != null) {
            String deliveryStreet = order.getDeliveryAddress().getStreet();

            if (!warehouseStreet.equals(deliveryStreet)) {
                cost += cost * 0.2;
            }
        }


        log.debug("Calculated delivery cost: {}", cost);
        return cost;
    }

    @Override
    @Transactional
    public void deliveryPicked(UUID orderId) {
        log.debug("Delivery picked for order: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order: " + orderId));

        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);

        orderClient.delivery(orderId);

        ShippedToDeliveryRequest request = new ShippedToDeliveryRequest();
        request.setOrderId(orderId);
        request.setDeliveryId(delivery.getDeliveryId());

        warehouseClient.shippedToDelivery(request);

        log.info("Delivery picked: {} for order: {}", delivery.getDeliveryId(), orderId);
    }

    @Override
    @Transactional
    public void deliverySuccessful(UUID orderId) {
        log.debug("Delivery successful for order: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order: " + orderId));

        delivery.setDeliveryState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);

        orderClient.delivery(orderId);

        log.info("Delivery successful: {} for order: {}", delivery.getDeliveryId(), orderId);
    }

    @Override
    @Transactional
    public void deliveryFailed(UUID orderId) {
        log.debug("Delivery failed for order: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order: " + orderId));

        delivery.setDeliveryState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);

        orderClient.deliveryFailed(orderId);

        log.info("Delivery failed: {} for order: {}", delivery.getDeliveryId(), orderId);
    }
}