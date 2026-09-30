package com.miyuki.Inventory.Management.product;

import com.miyuki.Inventory.Management.product.dto.CreateProductRequest;
import com.miyuki.Inventory.Management.product.dto.ProductResponse;
import com.miyuki.Inventory.Management.product.dto.UpdateProductRequest;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(CreateProductRequest request) {
        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setCategory(request.category());
        product.setReorderLevel(request.reorder_level());
        return product;
    }

    public void updateEntity(UpdateProductRequest request, Product product) {
        product.setSku(request.sku());
        product.setName(request.name());
        product.setCategory(request.category());
        product.setReorderLevel(request.reorder_level());
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getReorderLevel()
        );
    }
}
