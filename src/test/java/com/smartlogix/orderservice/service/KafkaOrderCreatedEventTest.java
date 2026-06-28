package com.smartlogix.orderservice.service;

import com.smartlogix.orderservice.dto.CreateOrderRequest;
import com.smartlogix.orderservice.dto.OrderItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**this test verifies that when we create an order, the system publishes an event to Kafka topic order-created.
 * @SpringBootTest = run full application context. “Run my app inside the test environment”, start the entire thing with real Beans (bc this is integration test, not just a unit test)
 * @EmbeddedKafka = fake kafka broker inside tests. It starts a real Kafka broker in-memory (for testing only). This way, we don’t need a real Kafka server running locally or in Docker.
 * @DirtiesContext =After the test finishes, destroy the Spring context "no leftovers from previous tests" (Kafka + Spring contexts can “leak state” between tests)
 * @MockitoBean = replace real dependency with mock
 * @KafkaListener=Spring method that reacts to Kafka messages
 * BlockingQueue= Thread-safe way to wait for async messages*/

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"order-created"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"  // Override the bootstrap-servers so the app uses the embedded broker. Without this line, the application would try to connect to real Kafka
)
@DirtiesContext //Reset Spring context after this test class
@Import(KafkaOrderCreatedEventTest.TestKafkaConsumer.class) //Spring scans the main source code, not test classes, but we need it to include this specific class as a bean.
public class KafkaOrderCreatedEventTest {

    @Autowired
    OrderService orderService;

    @MockitoBean
    RestTemplate restTemplate; //mock so stock reduction doesn't call real ProductService

    @Autowired
    TestKafkaConsumer testConsumer; // our spy that captures the message. TestKakfaConsumer is a little helper class we created inside the test that listens to the Kafka topic and captures messages.

    @Test
    void whenOrderCreated_eventPublishedToKafka() throws InterruptedException {

        // Mock RestTemplate so stock reduction succeeds silently
        when(restTemplate.postForObject(anyString(), any(), any()))
                .thenReturn(null);

        //build a create order request
        CreateOrderRequest request = new CreateOrderRequest();
        request.setClientId(1L);
        request.setShippingCompany("DHL");
        request.setShippingAddress("Av. Providencia 1234, Santiago");

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);
        request.setItems(List.of(item));

        //Create the order (this should publish to Kafka)
        orderService.createOrder(request);

        // Wait up to 5 seconds for the message to arrive
        String receivedMessage = testConsumer.messages.poll(5, TimeUnit.SECONDS);

        // Verify the message was published
        assertNotNull(receivedMessage, "NO MESSAGE FROM KAFKA TOPIC 'order-created'");
        System.out.println("MESSAGE RECEIVED: " + receivedMessage);

        //Verify it contains the right data
        assertTrue(receivedMessage.contains("\"shippingCompany\":\"DHL\""));
        assertTrue(receivedMessage.contains("\"shippingAddress\":\"Av. Providencia 1234, Santiago\""));
        assertTrue(receivedMessage.contains("\"orderId\""));

        System.out.println("KAFKA EVENT PUBLISHED SUCCESSFULLY!");
    }

    /**
     * A simple Kafka listener that captures messages for the test to inspect.
     * @Component makes it a Spring bean so it gets picked up automatically.
     * It uses a BlockingQueue: a thread-safe list that can wait for items to arrive.
     */
    @Component
    static class TestKafkaConsumer {
        // BlockingQueue.poll(5, SECONDS) = "wait up to 5 seconds for a message, then give up"
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        @KafkaListener(
                topics = "order-created",
                groupId = "test-consumer-group"
        )
        public void consume(String message) {
            System.out.println("[TestConsumer] Captured: " + message);
            messages.add(message);
        }
    }
}