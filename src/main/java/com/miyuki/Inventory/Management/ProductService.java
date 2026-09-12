package com.miyuki.Inventory.Management;

import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Optional;

@Service

public class ProductService {

    private final ProductRepository repository;
    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }


    public Product addProduct(Long id,String product_name,Long product_stock){
        Product p = new Product(id,product_name,product_stock);
        return repository.save(p);
    }
    public void deleteProduct(Long prod_id){
        repository.deleteById(prod_id);
    }

    public void buyProduct(Long prod_id,Long quantity){
        Product p = repository.findById(prod_id).orElseThrow();
        Long st =  p.getProduct_stock()-quantity;
        p.setProduct_stock(st);

    }

}
