package com.pharma.service;

import com.pharma.dto.StockSummary;
import com.pharma.exception.BusinessException;
import com.pharma.exception.NotFoundException;
import com.pharma.model.Batch;
import com.pharma.model.Product;
import com.pharma.repository.BatchRepository;
import com.pharma.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages per-batch stock. Purchases add stock into batches; sales remove stock
 * using a First-Expiry-First-Out (FEFO) strategy so near-expiry lots ship first.
 */
@Service
public class InventoryService {

    private final BatchRepository batchRepo;
    private final ProductRepository productRepo;

    public InventoryService(BatchRepository batchRepo, ProductRepository productRepo) {
        this.batchRepo = batchRepo;
        this.productRepo = productRepo;
    }

    public List<Batch> allBatches() {
        return batchRepo.findAll();
    }

    public List<Batch> batchesForProduct(Long productId) {
        return batchRepo.findByProductId(productId);
    }

    public int totalQuantity(Long productId) {
        return batchRepo.findByProductId(productId).stream()
                .mapToInt(Batch::getQuantity).sum();
    }

    /** Add stock to a batch, matching an existing lot number if one is given. */
    @Transactional
    public Batch addStock(Long productId, String lotNumber, LocalDate expiryDate, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("Quantity to add must be positive");
        }
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        Batch batch = null;
        if (lotNumber != null && !lotNumber.isBlank()) {
            batch = batchRepo.findByProductId(productId).stream()
                    .filter(b -> lotNumber.equalsIgnoreCase(b.getLotNumber()))
                    .findFirst().orElse(null);
        }
        if (batch == null) {
            batch = new Batch();
            batch.setProduct(product);
            batch.setLotNumber(lotNumber);
            batch.setExpiryDate(expiryDate);
            batch.setManufactureDate(LocalDate.now());
            batch.setQuantity(quantity);
        } else {
            batch.setQuantity(batch.getQuantity() + quantity);
            if (expiryDate != null) {
                batch.setExpiryDate(expiryDate);
            }
        }
        return batchRepo.save(batch);
    }

    /** Remove stock across batches using FEFO. Throws if not enough stock. */
    @Transactional
    public void removeStock(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("Quantity to remove must be positive");
        }
        List<Batch> batches = batchRepo
                .findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(productId, 0);
        int available = batches.stream().mapToInt(Batch::getQuantity).sum();
        if (available < quantity) {
            Product p = productRepo.findById(productId).orElse(null);
            String label = p != null ? p.getName() : ("#" + productId);
            throw new BusinessException("Insufficient stock for " + label
                    + ": requested " + quantity + ", available " + available);
        }
        int remaining = quantity;
        for (Batch b : batches) {
            if (remaining == 0) break;
            int take = Math.min(b.getQuantity(), remaining);
            b.setQuantity(b.getQuantity() - take);
            remaining -= take;
            batchRepo.save(b);
        }
    }

    public Batch updateBatch(Long batchId, Batch data) {
        Batch b = batchRepo.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + batchId));
        b.setLotNumber(data.getLotNumber());
        b.setExpiryDate(data.getExpiryDate());
        b.setManufactureDate(data.getManufactureDate());
        b.setQuantity(data.getQuantity());
        return batchRepo.save(b);
    }

    public void deleteBatch(Long batchId) {
        if (!batchRepo.existsById(batchId)) {
            throw new NotFoundException("Batch not found: " + batchId);
        }
        batchRepo.deleteById(batchId);
    }

    /** One aggregated row per product. */
    public List<StockSummary> stockSummary() {
        List<StockSummary> result = new ArrayList<>();
        for (Product p : productRepo.findAll()) {
            List<Batch> batches = batchRepo.findByProductId(p.getId());
            int qty = batches.stream().mapToInt(Batch::getQuantity).sum();
            LocalDate earliest = batches.stream()
                    .filter(b -> b.getQuantity() > 0 && b.getExpiryDate() != null)
                    .map(Batch::getExpiryDate)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            BigDecimal value = p.getPurchasePrice().multiply(BigDecimal.valueOf(qty));
            result.add(new StockSummary(p.getId(), p.getName(), p.getSku(), p.getForm(),
                    p.getStrength(), qty, p.getReorderLevel(), earliest, value));
        }
        return result;
    }

    public List<Batch> expiredBatches() {
        return batchRepo.findByExpiryDateBeforeAndQuantityGreaterThan(LocalDate.now(), 0);
    }

    public List<Batch> expiringSoon(int days) {
        LocalDate today = LocalDate.now();
        return batchRepo.findByExpiryDateBetweenAndQuantityGreaterThan(today, today.plusDays(days), 0);
    }
}
