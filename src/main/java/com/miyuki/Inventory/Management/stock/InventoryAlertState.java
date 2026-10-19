package com.miyuki.Inventory.Management.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_alert_state")
@Getter
@Setter
@NoArgsConstructor
public class InventoryAlertState {

    @Id
    @Column(name = "inventory_item_id")
    private Long inventoryItemId;

    @Column(nullable = false)
    private boolean lowStock;

    @Column(nullable = false)
    private boolean notificationSent;

    public InventoryAlertState(Long inventoryItemId, boolean lowStock, boolean notificationSent) {
        this.inventoryItemId = inventoryItemId;
        this.lowStock = lowStock;
        this.notificationSent = notificationSent;
    }
}
