package com.miyuki.Inventory.Management.product;


import com.miyuki.Inventory.Management.product.dto.CreateProductRequest;
import com.miyuki.Inventory.Management.product.dto.ProductResponse;
import com.miyuki.Inventory.Management.product.dto.UpdateProductRequest;
import org.springframework.stereotype.Service;

@Service
public class ProductService{

    private final ProductRepository productRepository;

   public ProductService(ProductRepository productRepository){
       this.productRepository = productRepository;
   }


   public ProductResponse createProduct(CreateProductRequest request){

       if(productRepository.existBySku(request.sku())){
           throw new RuntimeException("Product already exist");
       }

        Product  product = new Product();
       product.setSku(request.sku());
        product.setName(request.name());
        product.setReorder_level(request.reorder_level());

        Product saveProduct = productRepository.save(product);

        return mapToResponse(product);
   }


   public ProductResponse updateProduct(Long product_id, UpdateProductRequest newProduct){

       Product product = productRepository.findById(product_id).orElseThrow(
               () -> new RuntimeException("Product not found")
       );

       product.setName(newProduct.name());
       product.setSku(newProduct.sku());
       product.setReorder_level(newProduct.reorder_level());

       return mapToResponse(product);
   }
   public ProductResponse mapToResponse(Product product){

       return new ProductResponse(
               product.getId(),
               product.getSku(),
               product.getName(),
               product.getReorder_level(),
               product.getReorder_level() < 10
       );
   }
}