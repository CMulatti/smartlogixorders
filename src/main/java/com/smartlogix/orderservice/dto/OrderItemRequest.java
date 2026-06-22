//Nested inside CreateOrderRequest, represents one product line in the order (productId + quantity).
// Spring deserialises each item in the list into this DTO automatically.

package com.smartlogix.orderservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/** One line in the order: which product and how many. */
@Getter
@Setter
@NoArgsConstructor
public class OrderItemRequest {
    private Long productId;
    private Integer quantity;
}

