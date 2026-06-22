//incoming Kafka message received from ShipmentService when a shipment status changes.
// KafkaConsumerService in OrderService reads this and calls updateOrderStatusFromEvent() to keep the order in sync.

package com.smartlogix.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusChangedEvent {
    private Long orderId;
    private String newOrderStatus;
}