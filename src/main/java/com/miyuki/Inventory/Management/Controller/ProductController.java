package com.miyuki.Inventory.Management.Controller;

import com.miyuki.Inventory.Management.Model.Product;
import com.miyuki.Inventory.Management.Service.ProductService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService){
        this.productService = productService;
    }
    @PostMapping
    public String addProduct(@RequestBody Product p){
        productService.addProduct(p.getProduct_id(),p.getProduct_name(),p.getProduct_stock());
        return "Product Added Successfully";
    }
}
