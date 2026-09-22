package com.miyuki.Inventory.Management.stock;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Stock-Movement")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockMovement_id;

    private Long inventoryItem_id;
    private Long quantity_change;
    private Enum type;
}
