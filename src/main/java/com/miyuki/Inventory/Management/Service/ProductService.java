package com.miyuki.Inventory.Management.Service;

import com.miyuki.Inventory.Management.Model.Product;
import com.miyuki.Inventory.Management.Repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Product addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        return repository.save(product);
    }

    public Product getProduct(Long productId) {
        return repository.findById(productId).orElse(null);
    }

    public Product updateProductStock(Long productId, Long stock) {
        if (productId == null || stock == null) {
            throw new IllegalArgumentException("Product id and stock are required");
        }

        Product product = repository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        product.setProduct_stock(stock);
        return repository.save(product);
    }

    public String deleteProduct(Long productId) {
        if (productId == null) {
            return "Failed: Product id is required.";
        }

        if (!repository.existsById(productId)) {
            return "Failed: Product not found.";
        }

        repository.deleteById(productId);
        return "Product deleted successfully.";
    }
}
