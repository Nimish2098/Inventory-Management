package com.miyuki.Inventory.Management.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product", uniqueConstraints = @UniqueConstraint(columnNames = "sku"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;
    private String name;
    private String category;
    private Long reorderLevel;

}