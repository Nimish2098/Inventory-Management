package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.common.exception.InsufficientStockException;
import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.stock.dto.StockAdjustmentRequest;
import com.miyuki.Inventory.Management.stock.dto.StockTransferRequest;
import com.miyuki.Inventory.Management.stock.dto.InventoryItemResponse;
import com.miyuki.Inventory.Management.stock.dto.StockMovementResponse;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryitemService {

        private final InventoryitemRepository inventoryitemRepository;
        private final StockMovementRepository stockMovementRepository;
        private final ProductRepository productRepository;
        private final WarehouseRepository warehouseRepository;
        private final InventoryMapper inventoryMapper;

        public InventoryitemService(InventoryitemRepository inventoryitemRepository,
                                    StockMovementRepository stockMovementRepository,
                                    ProductRepository productRepository,
                                    WarehouseRepository warehouseRepository,
                                    InventoryMapper inventoryMapper){
            this.inventoryitemRepository = inventoryitemRepository;
            this.stockMovementRepository = stockMovementRepository;
            this.productRepository = productRepository;
            this.warehouseRepository = warehouseRepository;
            this.inventoryMapper = inventoryMapper;
        }

        @Transactional
        public InventoryItemResponse adjustStock(StockAdjustmentRequest request){
            validateAdjustment(request);
            var product = productRepository.findById(request.product_id())
                    .orElseThrow(() -> new ResourceNotFoundException("Product " + request.product_id() + " not found"));
            var warehouse = warehouseRepository.findById(request.warehouse_id())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse " + request.warehouse_id() + " not found"));

            InventoryItem item = inventoryitemRepository.findByProductIdAndWarehouseIdForUpdate(
                    request.product_id(), request.warehouse_id()).orElseGet(() -> {
                if (request.quantity() < 0) {
                    throw new InsufficientStockException("Inventory location has no stock");
                }
                InventoryItem newItem = new InventoryItem();
                newItem.setProduct(product);
                newItem.setWarehouse(warehouse);
                newItem.setQuantity(0L);
                return newItem;
            });
            long newQuantity = Math.addExact(item.getQuantity(), request.quantity());
            if(newQuantity < 0){
                throw new InsufficientStockException("Stock level cannot drop below 0");
            }
            item.setQuantity(newQuantity);
            item = inventoryitemRepository.save(item);
            recordMovement(item, request.quantity(), request.movementType());
            return inventoryMapper.toResponse(item);
        }

        @Transactional
        public List<InventoryItemResponse> transferStock(StockTransferRequest request) {
            if (request.product_id() == null || request.from_warehouse_id() == null ||
                    request.to_warehouse_id() == null || request.quantity() == null || request.quantity() <= 0) {
                throw new IllegalArgumentException("Product, source, destination, and a positive quantity are required");
            }
            if (request.from_warehouse_id().equals(request.to_warehouse_id())) {
                throw new IllegalArgumentException("Source and destination warehouses must differ");
            }

            var product = productRepository.findById(request.product_id())
                    .orElseThrow(() -> new ResourceNotFoundException("Product " + request.product_id() + " not found"));
            var fromWarehouse = warehouseRepository.findById(request.from_warehouse_id())
                    .orElseThrow(() -> new ResourceNotFoundException("Source warehouse not found"));
            var toWarehouse = warehouseRepository.findById(request.to_warehouse_id())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination warehouse not found"));
            InventoryItem source = inventoryitemRepository.findByProductIdAndWarehouseIdForUpdate(
                            request.product_id(), request.from_warehouse_id())
                    .orElseThrow(() -> new InsufficientStockException("Source inventory location has no stock"));
            if (source.getQuantity() < request.quantity()) {
                throw new InsufficientStockException("Insufficient stock for transfer");
            }
            InventoryItem destination = inventoryitemRepository.findByProductIdAndWarehouseIdForUpdate(
                    request.product_id(), request.to_warehouse_id()).orElseGet(() -> {
                InventoryItem newItem = new InventoryItem();
                newItem.setProduct(product);
                newItem.setWarehouse(toWarehouse);
                newItem.setQuantity(0L);
                return newItem;
            });

            source.setQuantity(source.getQuantity() - request.quantity());
            destination.setQuantity(Math.addExact(destination.getQuantity(), request.quantity()));
            source = inventoryitemRepository.save(source);
            destination = inventoryitemRepository.save(destination);
            recordMovement(source, -request.quantity(), StockMovementType.TRANSFER);
            recordMovement(destination, request.quantity(), StockMovementType.TRANSFER);
            return List.of(inventoryMapper.toResponse(source), inventoryMapper.toResponse(destination));
        }

        @Transactional(readOnly = true)
        public InventoryItemResponse getStockLevel(Long productId, Long warehouseId){
            InventoryItem item = inventoryitemRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found"));
            return inventoryMapper.toResponse(item);
        }

        @Transactional(readOnly = true)
        public List<InventoryItemResponse> getInventory(Long warehouseId) {
            List<InventoryItem> items = warehouseId == null
                    ? inventoryitemRepository.findAll()
                    : inventoryitemRepository.findByWarehouse_Id(warehouseId);
            return items.stream().map(inventoryMapper::toResponse).toList();
        }

        @Transactional(readOnly = true)
        public List<InventoryItemResponse> getLowStockAlerts(){
            return inventoryitemRepository.findLowStockItems().stream().map(inventoryMapper::toResponse).toList();
        }

        @Transactional(readOnly = true)
        public List<StockMovementResponse> getMovementHistory(Long productId, Long warehouseId) {
            if (!inventoryitemRepository.findByProductIdAndWarehouseId(productId, warehouseId).isPresent()) {
                throw new ResourceNotFoundException("Inventory location not found");
            }
            return stockMovementRepository.findByProductAndWarehouse(productId, warehouseId).stream()
                    .map(inventoryMapper::toResponse).toList();
        }

        private void recordMovement(InventoryItem item, long quantity, StockMovementType movementType) {
            StockMovement movement = new StockMovement();
            movement.setInventoryItem(item);
            movement.setQuantityChange(quantity);
            movement.setType(movementType);
            stockMovementRepository.save(movement);
        }

        private void validateAdjustment(StockAdjustmentRequest request) {
            if (request.product_id() == null || request.warehouse_id() == null || request.quantity() == null ||
                    request.quantity() == 0 || request.movementType() == null) {
                throw new IllegalArgumentException("Product, warehouse, non-zero quantity, and movement type are required");
            }
            if (request.movementType() == StockMovementType.TRANSFER) {
                throw new IllegalArgumentException("Use the transfer endpoint for stock transfers");
            }
            if ((request.movementType() == StockMovementType.INBOUND && request.quantity() < 0) ||
                    (request.movementType() == StockMovementType.OUTBOUND && request.quantity() > 0)) {
                throw new IllegalArgumentException("Quantity sign does not match movement type");
            }
        }
}
