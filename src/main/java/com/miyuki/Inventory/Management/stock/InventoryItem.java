package com.miyuki.Inventory.Management.stock;

import jakarta.persistence.*;
import com.miyuki.Inventory.Management.product.Product;
import com.miyuki.Inventory.Management.warehouses.Warehouse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory_item", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "warehouse_id"}))
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    private Long quantity;
    private String bin;
}
