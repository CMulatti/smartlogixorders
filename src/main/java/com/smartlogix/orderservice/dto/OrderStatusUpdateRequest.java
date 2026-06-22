// this was the DTO for the old REST endpoint that ShipmentService called via restTemplate.put().
// Since ShipmentService now uses Kafka instead, nobody sends HTTP requests to /orders/status anymore.
// The endpoint and this DTO are dead code, so it is left here disabled.


//package com.smartlogix.orderservice.dto;
//
//import lombok.Getter;
//import lombok.Setter;
//import lombok.NoArgsConstructor;
//
///** ShipmentService sends this when its status changes, asking OrderService to update the matching order's status.
// * Example:
// * {
// *   "orderId": 3,
// *   "newOrderStatus": "enviada"
// * }
// */
//@Getter
//@Setter
//@NoArgsConstructor
//public class OrderStatusUpdateRequest {
//    private Long orderId;
//    private String newOrderStatus;
//}
