package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.product.Product;
import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.user.UserAccount;
import com.miyuki.Inventory.Management.user.UserAccountRepository;
import com.miyuki.Inventory.Management.warehouses.Warehouse;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.security.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class LowStockAlertIsolationIntegrationTest {

    @Autowired
    private InventoryitemRepository inventoryRepository;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    private InventoryItem validItem;
    private InventoryItem crossAccountItem;
    private Product firstProduct;
    private Warehouse firstWarehouse;
    private Warehouse secondWarehouse;
    private UserAccount firstUser;
    private UserAccount secondUser;

    @AfterEach
    void cleanUp() {
        if (validItem != null) {
            inventoryRepository.delete(validItem);
        }
        if (crossAccountItem != null) {
            inventoryRepository.delete(crossAccountItem);
        }
        if (firstProduct != null) {
            productRepository.delete(firstProduct);
        }
        if (firstWarehouse != null) {
            warehouseRepository.delete(firstWarehouse);
        }
        if (secondWarehouse != null) {
            warehouseRepository.delete(secondWarehouse);
        }
        if (firstUser != null) {
            userRepository.delete(firstUser);
        }
        if (secondUser != null) {
            userRepository.delete(secondUser);
        }
    }

    @Test
    void monitoringQueryIncludesOnlyInventoryWhoseProductAndWarehouseShareAnOwner() {
        String unique = UUID.randomUUID().toString();
        firstUser = user("first-" + unique + "@example.test");
        secondUser = user("second-" + unique + "@example.test");
        firstProduct = new Product();
        firstProduct.setName("Owned product");
        firstProduct.setSku("MONITOR-P-" + unique);
        firstProduct.setReorderLevel(10L);
        firstProduct.setOwner(firstUser);
        firstProduct = productRepository.saveAndFlush(firstProduct);
        firstWarehouse = warehouse("MONITOR-W1-" + unique, firstUser);
        secondWarehouse = warehouse("MONITOR-W2-" + unique, secondUser);

        validItem = inventoryRepository.saveAndFlush(
                new InventoryItem(null, firstProduct, firstWarehouse, 2L, null));
        crossAccountItem = inventoryRepository.saveAndFlush(
                new InventoryItem(null, firstProduct, secondWarehouse, 1L, null));

        var monitoredIds = inventoryRepository.findAllForMonitoring().stream()
                .map(InventoryItem::getId)
                .toList();

        assertTrue(monitoredIds.contains(validItem.getId()));
        assertFalse(monitoredIds.contains(crossAccountItem.getId()));
    }

    private UserAccount user(String email) {
        UserAccount user = new UserAccount();
        user.setName("Monitoring test");
        user.setEmail(email);
        user.setPassword("unused-test-hash");
        return userRepository.saveAndFlush(user);
    }

    private Warehouse warehouse(String code, UserAccount owner) {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(code);
        warehouse.setName(code);
        warehouse.setActive(true);
        warehouse.setOwner(owner);
        return warehouseRepository.saveAndFlush(warehouse);
    }
}
