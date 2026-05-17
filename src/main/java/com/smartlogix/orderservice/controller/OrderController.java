package com.smartlogix.orderservice.controller;

import com.smartlogix.orderservice.dto.CreateOrderRequest;
import com.smartlogix.orderservice.dto.OrderStatusUpdateRequest;
import com.smartlogix.orderservice.entity.Order;
import com.smartlogix.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    //POST /orders  in OrderService: it creates an order, asks PRODUCTSERVICE to reduce stock, asks SHIPMENT SERVICE to create shipment.
    // This corresponds to "generate order" button in the frontend simulating 3rd party b2b incoming data.
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }


    //PUT orders/status  internal endpoint called by SHIPMENTSERVICE. SHIPMENTSERVICE ("orderId": 1, "newOrderStatus": "enviada")
    @PutMapping("/status")
    public ResponseEntity<Order> updateOrderStatus(@RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(request));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
