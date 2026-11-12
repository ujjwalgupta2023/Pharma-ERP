package com.pharma.repository;

import com.pharma.model.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    List<SalesOrder> findAllByOrderByOrderDateDesc();
    List<SalesOrder> findByStatusAndOrderDateGreaterThanEqual(String status, LocalDate from);
}
