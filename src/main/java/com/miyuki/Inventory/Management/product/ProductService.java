package com.miyuki.Inventory.Management.product;


import org.springframework.stereotype.Service;

@Service
public class ProductService{

    private final ProductRepository productRepository;

    //Constructor Injection
    public ProductService(ProductRepository productRepository){
        this.productRepository = productRepository;
    }


    public Product addProduct(Product product){
        return productRepository.save(product);
    }

    public String deleteProduct(Long productId){

        if(!productRepository.existsById(productId)){
            return "No Such Product Found.";
        }
        productRepository.deleteById(productId);
        return "Product Deleted Successfully";
    }


}