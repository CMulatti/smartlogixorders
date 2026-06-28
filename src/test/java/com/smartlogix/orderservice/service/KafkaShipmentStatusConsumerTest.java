package com.smartlogix.orderservice.service;

import com.smartlogix.orderservice.entity.Order;
import com.smartlogix.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;


/** this tests verifies that OrderService correctly CONSUMES the "shipment-status-changed" Kafka topic.
 * Flow being tested:
 *   ShipmentService publishes event to "shipment-status-changed":
 *     OrderService KafkaConsumerService picks it up
 *     orderService.updateOrderStatusFromEvent() is called
 *      Order status is updated in DB
 * We simulate ShipmentService by publishing directly with KafkaTemplate in the test.
 */

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"shipment-status-changed", "order-created"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@DirtiesContext
public class KafkaShipmentStatusConsumerTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    // We use KafkaTemplate to PUBLISH the event (simulating ShipmentService)
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    //Mock RestTemplate so no real HTTP calls go out during context load
    @MockitoBean
    RestTemplate restTemplate;

    @Test
    void whenShipmentStatusChanged_orderStatusUpdates() throws InterruptedException {

        //1: CREATE A REAL ORDER IN DB SO THAT WE HAVE STH TO UPDATE
        Order order = new Order();
        order.setClientId(1L);
        order.setOrderStatus("creada");
        Order savedOrder = orderRepository.save(order);

        System.out.println("Created order #" + savedOrder.getOrderId() + " with status: creada");

        //2. SIMULATE SHIPMENTSERVICE PUBLISHING THE EVENT
        // This is exactly the JSON that ShipmentService.notifyOrderService() produces
        String eventJson = String.format(
                "{\"orderId\":%d,\"newOrderStatus\":\"enviada\"}",
                savedOrder.getOrderId()
        );

        kafkaTemplate.send("shipment-status-changed", eventJson);
        System.out.println("Published event: " + eventJson);

        //3. WAIT FOR ORDERSERVICE'S KafkaConsumerService TO PROCESS IT.
        TimeUnit.SECONDS.sleep(3); //Kafka is async so we give it a few seconds

        //4. RELOAD ORDER FROM DB AND VERIFY STATUS CHANGED
        Order updatedOrder = orderRepository.findById(savedOrder.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        System.out.println("Order status after event: " + updatedOrder.getOrderStatus());

        assertEquals("enviada", updatedOrder.getOrderStatus(),
                "Order status should have been updated to 'enviada' by the Kafka consumer");


        System.out.println("TEST PASSED: ORDERSERVICE CONSUMED shipment-status-changed EVENT CORRECTLY!");
        System.out.println("Order #" + savedOrder.getOrderId() + " updated: creada -> enviada");

    }
}