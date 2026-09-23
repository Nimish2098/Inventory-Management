package com.miyuki.Inventory.Management.stock;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Stock-Movement")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockMovement_id;

    private InventoryItem inventoryItem;
    private Long quantity_change;
    private Enum type;
}
