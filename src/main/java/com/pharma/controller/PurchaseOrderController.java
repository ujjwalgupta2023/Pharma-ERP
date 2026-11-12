package com.pharma.controller;

import com.pharma.model.PurchaseOrder;
import com.pharma.service.PurchaseOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@CrossOrigin
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    public PurchaseOrderController(PurchaseOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<PurchaseOrder> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PurchaseOrder one(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<PurchaseOrder> create(@RequestBody PurchaseOrder order) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(order));
    }

    @PostMapping("/{id}/receive")
    public PurchaseOrder receive(@PathVariable Long id) {
        return service.receive(id);
    }

    @PostMapping("/{id}/cancel")
    public PurchaseOrder cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
