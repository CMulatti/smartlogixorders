//outgoing Kafka message published to the order-created topic after an order is saved.
// ShipmentService reads this to create the shipment automatically.

package com.smartlogix.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** This is the MESSAGE we publish to Kafka when an order is created. SHIPMENTSERVICE will receive this exact object and use it to create a shipment.
This is like an envelope we drop into a mailbox (Kafka topic), SHIPMENTSERVICE is waiting at the other end to pick it up.
 * It needs @NoArgsConstructor because Kafka's deserialiser needs to create an empty object first, then fill in the fields.
 * It needs @AllArgsConstructor so we can build it easily on the sending side.*/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private String shippingCompany;
    private String shippingAddress;
}
