package com.pharma.repository;

import com.pharma.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
            String name, String genericName, String sku);
}
