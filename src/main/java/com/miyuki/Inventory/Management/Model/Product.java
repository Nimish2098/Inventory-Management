package com.miyuki.Inventory.Management.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="users")
public class Product {
    @Id
    private Long product_id;
    private String product_name;
    private Long product_stock;

    public Product(Long productId, String product_name, Long product_stock) {
        this.product_id = productId;
        this.product_name = product_name;
        this.product_stock = product_stock;
    }

    public Product() {
    }

    public Long getProduct_id() {return product_id;}

    public void setProduct_id(Long product_id) {this.product_id = product_id;}

    public String getProduct_name() {
        return product_name;
    }

    public void setProduct_name(String product_name) {
        this.product_name = product_name;
    }

    public Long getProduct_stock() {
        return product_stock;
    }

    public void setProduct_stock(Long product_stock) {
        this.product_stock = product_stock;
    }
}
