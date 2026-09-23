package com.miyuki.Inventory.Management.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="Product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku; //Stock Keeping Unit : A unique code which represents product
    private String name;
    private Long reorder_level;

}