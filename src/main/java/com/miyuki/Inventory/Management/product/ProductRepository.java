package com.miyuki.Inventory.Management.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository  extends JpaRepository<Product,Long> {

    Optional<Product> findByIdAndOwner_Id(Long id, Long ownerId);
    Optional<Product> findBySkuAndOwner_Id(String sku, Long ownerId);
    boolean existsBySkuAndOwner_Id(String sku, Long ownerId);
    List<Product> findAllByOwner_Id(Long ownerId);

    // Bulk lookup: find existing SKUs for one user.
    List<Product> findAllByOwner_IdAndSkuIn(Long ownerId,List<String> skus);
}
