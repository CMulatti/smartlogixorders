//This DTO was for the previous Rest call
// ShipmentService no longer sends an HTTP request to OrderService. Now it's handled by Kafka


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
