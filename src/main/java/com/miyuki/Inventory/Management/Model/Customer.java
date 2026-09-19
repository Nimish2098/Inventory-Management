package com.miyuki.Inventory.Management.Model;


import jakarta.persistence.*;

@Entity
@Table(name= "Customer")
public class Customer{

    @Id
    @GeneratedValue (strategy =  GenerationType.IDENTITY)
    private Long customer_id;
    private String customer_email;
    private String customer_address;
    public Long getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(Long customer_id) {
        this.customer_id = customer_id;
    }

    public String getCustomer_address() {
        return customer_address;
    }

    public void setCustomer_address(String customer_address) {
        this.customer_address = customer_address;
    }

    public String getCustomer_email() {
        return customer_email;
    }

    public void setCustomer_email(String customer_email) {
        this.customer_email = customer_email;
    }

    public Customer(){

    }
    public Customer(Long customer_id, String customer_email, String customer_address) {
        this.customer_id = customer_id;
        this.customer_email = customer_email;
        this.customer_address = customer_address;
    }




}