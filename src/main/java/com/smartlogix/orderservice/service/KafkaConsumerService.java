package com.smartlogix.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlogix.orderservice.dto.ShipmentStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "shipment-status-changed", groupId = "order-service-group")
    public void handleShipmentStatusChanged(String payload) {
        try {
            System.out.println("[OrderService] Raw event received: " + payload);

            ShipmentStatusChangedEvent event = objectMapper.readValue(payload, ShipmentStatusChangedEvent.class);

            orderService.updateOrderStatusFromEvent(event.getOrderId(), event.getNewOrderStatus());

            System.out.println("[OrderService] Order " + event.getOrderId()
                    + " updated to '" + event.getNewOrderStatus() + "'");

        } catch (Exception e) {
            System.err.println("[OrderService] ERROR processing event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}