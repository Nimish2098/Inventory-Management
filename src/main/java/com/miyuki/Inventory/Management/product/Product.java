package com.miyuki.Inventory.Management.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="Product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku; //Stock Keeping Unit : A unique code which represents product
    private String name;
    private Long reorder_level;

}