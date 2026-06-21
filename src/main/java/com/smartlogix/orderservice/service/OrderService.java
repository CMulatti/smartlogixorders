package com.smartlogix.orderservice.service;

import com.smartlogix.orderservice.dto.CreateOrderRequest;
import com.smartlogix.orderservice.dto.OrderItemRequest;
import com.smartlogix.orderservice.dto.OrderStatusUpdateRequest;
import com.smartlogix.orderservice.entity.Order;
import com.smartlogix.orderservice.entity.OrderItem;
import com.smartlogix.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.smartlogix.orderservice.dto.OrderCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate; //injected from RestTemplateConfig, we are still calling PRODUCTSERVICE with this

    private final KafkaTemplate<String, String> kafkaTemplate;

    //Topic name as a constant so it's easy to find and change
    private static final String ORDER_CREATED_TOPIC = "order-created";

    private final ExternalApiService externalApiService;

    @Value("${app.productservice.url}") //the urls from aṕplication.properties @Value("${property.key}") reads a property at startup.
    private String productServiceUrl;

//    @Value("${app.shipmentservice.url}")
//    private String shipmentServiceUrl;


    //--------- READ ----------------------------------------------------------------------------

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido con id: " + id + " no encontrado"));
    }

    //----------- CREATE ORDER ------------------------------------------------------------------
    public Order createOrder(CreateOrderRequest request) {

        //--- STEP 1: BUILD THE ENTITIES ----
        // Build Order entity
        Order order = new Order();
        order.setClientId(request.getClientId());
        order.setOrderStatus("creada"); //always starts as "creada"

        //Build OrderItem entities and link them to the order
        for (OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemReq.getProductId());
            item.setQuantity(itemReq.getQuantity());
            item.setOrder(order); // link child → parent
            order.getItems().add(item); //link parent → child
        }


        //--- STEP 2: RestTemplate HTTP COMMUNICATION ------
        // First check stock with PRODUCTSERVICE (before saving)
        for (OrderItemRequest itemReq : request.getItems()) {
            String productUrl = productServiceUrl + "/products/reduce-stock";

            java.util.Map<String, Object> stockRequest = new java.util.HashMap<>(); //we could create a DTO for this instead, but for now this is ok. the DTO gives as compiler protection, the Map version doesn't protect us if we make a typo "prodcutID" instead of "productId" for example.
            stockRequest.put("productId", itemReq.getProductId());
            stockRequest.put("quantity", itemReq.getQuantity());

            //restTemplate.postForObject(productUrl, stockRequest, Object.class); //Spring converts the map into JSON automatically
            externalApiService.notifyProductService(productUrl, stockRequest); // now we replaced the direct restTemplate call in the line above with the new method from circuit breaker
        }

        //--- STEP 3: Only save the order if ALL stock reductions succeeded
        Order savedOrder = orderRepository.save(order);

//      // ------------------------------ no longer in use -------------------------------------------------------------
//        Tell SHIPMENTSERVICE to create a shipment for this order (via HTTP).
//        String shipmentUrl = shipmentServiceUrl + "/shipments";
//
//        java.util.Map<String, Object> shipmentRequest = new java.util.HashMap<>();
//        shipmentRequest.put("orderId", savedOrder.getOrderId());
//        shipmentRequest.put("shippingCompany", request.getShippingCompany());
//        shipmentRequest.put("shippingAddress", request.getShippingAddress());
//        //shipmentStatus defaults to "pendiente" on SHIPMENTSERVICE side
//
//        restTemplate.postForObject(shipmentUrl, shipmentRequest, Object.class);
        //--------------------------------------------------------------------------------------------------------------


        //Publish event to Kafka
        // We build the event object (the "message envelope")
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getOrderId(),
                request.getShippingCompany(),
                request.getShippingAddress()
        );

        //kafkaTemplate.send(topicName, message)
        //serialises the event to JSON, drops it into the "order-created" topic and returns immediately. No wait for SHIPMENTSERVICE.
        // SHIPMENTSERVICE will pick it up on its own, whenever it's ready*/
        String eventJson = String.format(
                "{\"orderId\":%d,\"shippingCompany\":\"%s\",\"shippingAddress\":\"%s\"}",
                savedOrder.getOrderId(),
                request.getShippingCompany(),
                request.getShippingAddress()
        );
        kafkaTemplate.send(ORDER_CREATED_TOPIC, eventJson);

        System.out.println("[OrderService] Event published to Kafka topic '" + ORDER_CREATED_TOPIC + "' for orderId: " + savedOrder.getOrderId());

        return savedOrder;
    }

    //------------- UPDATE ORDER STATUS (called by SHIPMENTSERVICE via RestTemplate) ---------------------
    /** SHIPMENTSERVICE calls this endpoint (via RestTemplate) when its status changes.
     * It tells US which order to update and what the new status should be.
     * Valid statuses: creada → enviada → completada
     * We do NOT call SHIPMENTSERVICE back from here, that would create a loop! Clear ownership: ORDERSERVICE owns order_status.
     * The flow is like this:
     * 1. SHIPMENT SERVICE sends HTTP request
     * 2. OrderController receives it
     * 3. Spring converts JSON to an OrderStatusUpdateRequest DTO object
     * 4. OrderController calls orderService.updateOrderStatus(request)
     * 5. OrderService fetches order from DB, updates it status, and saves it to DB via orderRepository */
    public Order updateOrderStatus(OrderStatusUpdateRequest request) {
        Order order = getOrderById(request.getOrderId());
        order.setOrderStatus(request.getNewOrderStatus());
        return orderRepository.save(order);
    }

    //---------------------- DELETE ----------------------------------------------
    public void deleteOrder(Long id) {
        getOrderById(id);
        orderRepository.deleteById(id);
    }
    //We should consider adding rules that only orders with "creada" status can be deleted, so that managers don't delete when shipment is already on its way
}
