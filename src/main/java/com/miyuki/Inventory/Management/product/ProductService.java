package com.miyuki.Inventory.Management.product;


import com.miyuki.Inventory.Management.product.dto.CreateProductRequest;
import com.miyuki.Inventory.Management.product.dto.ProductResponse;
import com.miyuki.Inventory.Management.product.dto.UpdateProductRequest;
import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.user.CurrentUser;
import com.miyuki.Inventory.Management.user.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService{

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CurrentUser currentUser;
    private final UserAccountRepository userRepository;

   public ProductService(ProductRepository productRepository, ProductMapper productMapper,
                         CurrentUser currentUser, UserAccountRepository userRepository){
       this.productRepository = productRepository;
       this.productMapper = productMapper;
       this.currentUser = currentUser;
       this.userRepository = userRepository;
   }


   public ProductResponse createProduct(CreateProductRequest request){

       validate(request.sku(), request.name(), request.reorder_level());
       if(productRepository.existsBySkuAndOwner_Id(request.sku(), currentUser.id())){
           throw new IllegalStateException("Product SKU already exists");
       }
       Product product = productMapper.toEntity(request);
       product.setOwner(userRepository.findById(currentUser.id())
               .orElseThrow(() -> new ResourceNotFoundException("User account not found")));
       return productMapper.toResponse(productRepository.save(product));
   }


   public ProductResponse updateProduct(Long productId, UpdateProductRequest request){
       validate(request.sku(), request.name(), request.reorder_level());
       Product product = getEntity(productId);
       productRepository.findBySkuAndOwner_Id(request.sku(), currentUser.id())
               .filter(existing -> !existing.getId().equals(productId))
               .ifPresent(existing -> { throw new IllegalStateException("Product SKU already exists"); });
       productMapper.updateEntity(request, product);
       return productMapper.toResponse(productRepository.save(product));
   }

   public ProductResponse getProduct(Long productId) {
       return productMapper.toResponse(getEntity(productId));
   }

   public List<ProductResponse> getProducts() {
       return productRepository.findAllByOwner_Id(currentUser.id()).stream().map(productMapper::toResponse).toList();
   }

   public void deleteProduct(Long productId) {
       productRepository.delete(getEntity(productId));
   }

   private Product getEntity(Long productId) {
       return productRepository.findByIdAndOwner_Id(productId, currentUser.id())
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