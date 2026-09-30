package com.miyuki.Inventory.Management.product;

import com.miyuki.Inventory.Management.product.dto.CreateProductRequest;
import com.miyuki.Inventory.Management.product.dto.ProductResponse;
import com.miyuki.Inventory.Management.product.dto.UpdateProductRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {


    private final ProductService productService;

    public ProductController(ProductService productService){
        this.productService = productService;
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@RequestBody CreateProductRequest request){
        return productService.createProduct(request);
    }

    @GetMapping
    public List<ProductResponse> getProducts() {
        return productService.getProducts();
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @RequestBody UpdateProductRequest product){
        return productService.updateProduct(id,product);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}