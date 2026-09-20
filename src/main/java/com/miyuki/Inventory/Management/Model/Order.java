package com.miyuki.Inventory.Management.Model;

import jakarta.persistence.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long order_id;
    private Long customer_id;
    @ManyToMany
    private List<Product> cart;
    @ElementCollection
    @CollectionTable(name = "order_item_quantities", joinColumns = @JoinColumn(name = "order_id"))
    @MapKeyColumn(name = "product_id")
    @Column(name = "quantity")
    private Map<Long, Integer> itemQuantities = new HashMap<>();
    private String order_status;
    private Double totalAmount;

    public Order() {
    }

    public Order(Long order_id, Long customer_id, List<Product> cart, String order_status, Double totalAmount) {
        this.order_id = order_id;
        this.customer_id = customer_id;
        this.cart = cart;
        this.order_status = order_status;
        this.totalAmount = totalAmount;
    }

    public Long getOrder_id() {
        return order_id;
    }

    public void setOrder_id(Long order_id) {
        this.order_id = order_id;
    }

    public Long getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(Long customer_id) {
        this.customer_id = customer_id;
    }

    public List<Product> getCart() {
        return cart;
    }

    public void setCart(List<Product> cart) {
        this.cart = cart;
    }

    public Map<Long, Integer> getItemQuantities() {
        return itemQuantities;
    }

    public void setItemQuantities(Map<Long, Integer> itemQuantities) {
        this.itemQuantities = itemQuantities;
    }

    public String getOrder_status() {
        return order_status;
    }

    public void setOrder_status(String order_status) {
        this.order_status = order_status;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}