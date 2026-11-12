package com.pharma.controller;

import com.pharma.dto.StockSummary;
import com.pharma.model.Batch;
import com.pharma.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public List<StockSummary> summary() {
        return service.stockSummary();
    }

    @GetMapping("/batches")
    public List<Batch> batches(@RequestParam(required = false) Long productId) {
        return productId != null ? service.batchesForProduct(productId) : service.allBatches();
    }

    @GetMapping("/expired")
    public List<Batch> expired() {
        return service.expiredBatches();
    }

    @GetMapping("/expiring-soon")
    public List<Batch> expiringSoon(@RequestParam(defaultValue = "90") int days) {
        return service.expiringSoon(days);
    }

    /** Manual stock adjustment / goods receipt. */
    @PostMapping("/adjust")
    public Batch adjust(@RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(String.valueOf(body.get("productId")));
        String lotNumber = body.get("lotNumber") != null ? String.valueOf(body.get("lotNumber")) : null;
        LocalDate expiry = body.get("expiryDate") != null && !String.valueOf(body.get("expiryDate")).isBlank()
                ? LocalDate.parse(String.valueOf(body.get("expiryDate"))) : null;
        int quantity = Integer.parseInt(String.valueOf(body.get("quantity")));
        return service.addStock(productId, lotNumber, expiry, quantity);
    }

    @PutMapping("/batches/{id}")
    public Batch updateBatch(@PathVariable Long id, @RequestBody Batch batch) {
        return service.updateBatch(id, batch);
    }

    @DeleteMapping("/batches/{id}")
    public ResponseEntity<Void> deleteBatch(@PathVariable Long id) {
        service.deleteBatch(id);
        return ResponseEntity.noContent().build();
    }
}
