package com.miyuki.Inventory.Management.product;


import com.miyuki.Inventory.Management.product.dto.CreateProductRequest;
import com.miyuki.Inventory.Management.product.dto.ProductResponse;
import com.miyuki.Inventory.Management.product.dto.UpdateProductRequest;
import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService{

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

   public ProductService(ProductRepository productRepository, ProductMapper productMapper){
       this.productRepository = productRepository;
       this.productMapper = productMapper;
   }


   public ProductResponse createProduct(CreateProductRequest request){

       validate(request.sku(), request.name(), request.reorder_level());
       if(productRepository.existsBySku(request.sku())){
           throw new IllegalStateException("Product SKU already exists");
       }
       return productMapper.toResponse(productRepository.save(productMapper.toEntity(request)));
   }


   public ProductResponse updateProduct(Long productId, UpdateProductRequest request){
       validate(request.sku(), request.name(), request.reorder_level());
       Product product = getEntity(productId);
       productRepository.findBySku(request.sku()).filter(existing -> !existing.getId().equals(productId))
               .ifPresent(existing -> { throw new IllegalStateException("Product SKU already exists"); });
       productMapper.updateEntity(request, product);
       return productMapper.toResponse(productRepository.save(product));
   }

   public ProductResponse getProduct(Long productId) {
       return productMapper.toResponse(getEntity(productId));
   }

   public List<ProductResponse> getProducts() {
       return productRepository.findAll().stream().map(productMapper::toResponse).toList();
   }

   public void deleteProduct(Long productId) {
       productRepository.delete(getEntity(productId));
   }

   private Product getEntity(Long productId) {
       return productRepository.findById(productId)
               .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " not found"));
   }

   private void validate(String sku, String name, Long reorderLevel) {
       if (sku == null || sku.isBlank() || name == null || name.isBlank()) {
           throw new IllegalArgumentException("SKU and name are required");
       }
       if (reorderLevel == null || reorderLevel < 0) {
           throw new IllegalArgumentException("Reorder level must be zero or greater");
       }
   }
}