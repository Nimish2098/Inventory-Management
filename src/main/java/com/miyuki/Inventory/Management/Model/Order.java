package com.miyuki.Inventory.Management.Model;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table (name = "Order")
public class Order{

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long order_id;
    private Long customer_id;
    private List<Product> cart;
    private String order_status;

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

    public String getOrder_status() {
        return order_status;
    }

    public void setOrder_status(String order_status) {
        this.order_status = order_status;
    }

    public Order(Long order_id, Long customer_id, List<Product> cart, String order_status) {
        this.order_id = order_id;
        this.customer_id = customer_id;
        this.cart = cart;
        this.order_status = order_status;
    }

    public Order(){

    }
}