package com.smartlogix.orderservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *Example JSON for a new order
 * {
 *   "clientId": 1,
 *   "items": [
 *     { "productId": 1, "quantity": 2 },
 *     { "productId": 3, "quantity": 1 }
 *   ],
 *   "shippingCompany": "ChileExpress",
 *   "shippingAddress": "Antonio Varas 666"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateOrderRequest {
    private Long clientId;
    private List<OrderItemRequest> items;
    private String shippingCompany;
    private String shippingAddress;
}
