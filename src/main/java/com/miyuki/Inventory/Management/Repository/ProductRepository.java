package com.miyuki.Inventory.Management.Repository;

import com.miyuki.Inventory.Management.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository  extends JpaRepository<Product,Long> {


}
