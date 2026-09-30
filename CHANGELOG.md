# Change Log

## 2026-09-30: Complete Product, Warehouse, and Inventory API

### What changed and why

- `src/main/java/com/miyuki/Inventory/Management/product/Product.java` and `ProductRepository.java`: normalized the entity field/repository property names and enforce unique product SKUs so persistence queries and the product API use one consistent model.
- `product/ProductMapper.java`, `product/ProductService.java`, `product/ProductController.java`, and `product/dto/`: added explicit request/entity/response conversion, required-field and reorder-level validation, duplicate-SKU checks, and product create/list/get/update/delete endpoints. Product updates now persist, and updates use `PUT /product/{id}` rather than the previously unmapped `/id` route.
- `warehouses/Warehouse.java`, `WarehouseRepository.java`, `WarehouseMapper.java`, `WarehouseService.java`, `WarehouseController.java`, and `warehouses/dto/`: added explicit DTO mapping, unique warehouse codes, validation, active warehouse listing, and create/get/update/delete endpoints. Creation now reads JSON from the request body; updates replace warehouse details rather than accidentally adding to capacity.
- `stock/InventoryItem.java`, `StockMovement.java`, `InventoryitemRepository.java`, `StockMovementRepository.java`, `InventoryMapper.java`, `InventoryitemService.java`, `InventoryitemController.java`, and `stock/dto/`: modeled each inventory location as a product/warehouse pair; added typed movement records; repaired the transfer and inventory response DTOs; and completed endpoints for adjustments, transfers, inventory lookup/listing, low-stock alerts, and movement history.
- Stock adjustments and transfers are transactional so quantity changes and their movement records commit or roll back together. Stock cannot become negative, inbound/outbound signs are validated, and transfer source/destination warehouses must differ.
- `common/exception/GlobalExceptionHandler.java`: mapped missing resources, invalid requests, and conflicts to HTTP 404, 400, and 409 responses.
- `README.md`: replaced obsolete customer/order documentation with the implemented domain model, route reference, request semantics, and data migration note.

### Schema compatibility

Inventory now has a generated location ID and a unique `(product_id, warehouse_id)` pair; stock movements reference an inventory location. The table names are normalized to `inventory_item` and `stock_movement`. Existing databases with the earlier inventory model must be backed up and migrated before using this version; Hibernate `ddl-auto=update` does not migrate the old inventory data or primary key safely.

### Verification

- VS Code diagnostics reported no errors in the production Java source after the changes.
- Maven tests were not run. The existing `src/test/java/com/miyuki/Inventory/Management/OrderServiceTest.java` imports customer/order packages that are absent from `src/main`, so the test source needs cleanup before it can validate this API work.
