package com.pharma.repository;

import com.pharma.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByProductId(Long productId);

    /** Batches with stock for a product, earliest expiry first (FEFO). */
    List<Batch> findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(Long productId, int quantity);

    List<Batch> findByExpiryDateBeforeAndQuantityGreaterThan(LocalDate date, int quantity);

    List<Batch> findByExpiryDateBetweenAndQuantityGreaterThan(LocalDate start, LocalDate end, int quantity);
}
