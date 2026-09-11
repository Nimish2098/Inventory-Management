package com.miyuki.Inventory.Management;

import jakarta.persistence.Entity;

@Entity
public class Product {

    private String product_name;
    private Long product_stock;

    public Product(Long product_stock, String product_name) {
        this.product_stock = product_stock;
        this.product_name = product_name;
    }
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
