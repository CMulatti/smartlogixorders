package com.smartlogix.orderservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    //by default matches 'creada' in the DB
    @Column(name = "order_status", length = 20)
    private String orderStatus = "creada";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    //OneToMany: one order has many order items.
    //mappedBy: the "order" field inside OrderItem owns the FK column
    //cascadeType.ALL : if we save the Order, the items are also saved.
    //                  if we delete the Order, the items are also deleted.
    // orphanRemoval: if an item is removed from the list, it's deleted from DB too.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
