package com.miyuki.Inventory.Management;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;

@Repository
public interface ProductRepository  extends JpaRepository<Product,Long> {


}
