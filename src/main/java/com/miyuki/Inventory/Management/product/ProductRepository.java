package com.miyuki.Inventory.Management.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository  extends JpaRepository<Product,Long> {

    Optional<Product> findBySku(String sku);
    boolean existBySku(String sku);
    // findByCategoryId(Long category_id);
}
