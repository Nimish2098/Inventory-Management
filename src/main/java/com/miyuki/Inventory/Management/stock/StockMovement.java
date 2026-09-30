package com.miyuki.Inventory.Management.stock;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stock_movement")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;
    private Long quantityChange;

    @Enumerated(EnumType.STRING)
    private StockMovementType type;
}
