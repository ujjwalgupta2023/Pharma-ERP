package com.pharma.controller;

import com.pharma.dto.InvoiceView;
import com.pharma.model.SalesOrder;
import com.pharma.service.SalesOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-orders")
@CrossOrigin
public class SalesOrderController {

    private final SalesOrderService service;

    public SalesOrderController(SalesOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalesOrder> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SalesOrder one(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/invoice")
    public InvoiceView invoice(@PathVariable Long id) {
        return service.invoice(id);
    }

    @PostMapping
    public ResponseEntity<SalesOrder> create(@RequestBody SalesOrder order) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(order));
    }

    @PostMapping("/{id}/confirm")
    public SalesOrder confirm(@PathVariable Long id) {
        return service.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public SalesOrder cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
