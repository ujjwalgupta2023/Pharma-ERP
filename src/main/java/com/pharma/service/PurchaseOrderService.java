package com.pharma.service;

import com.pharma.exception.BusinessException;
import com.pharma.exception.NotFoundException;
import com.pharma.model.*;
import com.pharma.repository.ProductRepository;
import com.pharma.repository.PurchaseOrderRepository;
import com.pharma.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository repo;
    private final SupplierRepository supplierRepo;
    private final ProductRepository productRepo;
    private final InventoryService inventoryService;

    public PurchaseOrderService(PurchaseOrderRepository repo, SupplierRepository supplierRepo,
                                ProductRepository productRepo, InventoryService inventoryService) {
        this.repo = repo;
        this.supplierRepo = supplierRepo;
        this.productRepo = productRepo;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> findAll() {
        List<PurchaseOrder> orders = repo.findAllByOrderByOrderDateDesc();
        orders.forEach(o -> o.getItems().size()); // initialize within session
        return orders;
    }

    @Transactional(readOnly = true)
    public PurchaseOrder findById(Long id) {
        PurchaseOrder order = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase order not found: " + id));
        order.getItems().size(); // initialize within session
        return order;
    }

    @Transactional
    public PurchaseOrder create(PurchaseOrder order) {
        order.setId(null);
        order.setStatus("DRAFT");

        if (order.getSupplier() != null && order.getSupplier().getId() != null) {
            Supplier supplier = supplierRepo.findById(order.getSupplier().getId())
                    .orElseThrow(() -> new NotFoundException("Supplier not found: " + order.getSupplier().getId()));
            order.setSupplier(supplier);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : order.getItems()) {
            Product product = productRepo.findById(item.getProduct().getId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + item.getProduct().getId()));
            item.setProduct(product);
            item.setPurchaseOrder(order);
            total = total.add(item.getLineTotal());
        }
        order.setTotal(total);
        PurchaseOrder saved = repo.save(order);
        if (saved.getOrderNumber() == null || saved.getOrderNumber().isBlank()) {
            saved.setOrderNumber("PO-" + String.format("%05d", saved.getId()));
            saved = repo.save(saved);
        }
        return saved;
    }

    /** Mark as received and push all line items into inventory as batches. */
    @Transactional
    public PurchaseOrder receive(Long id) {
        PurchaseOrder order = findById(id);
        if ("RECEIVED".equals(order.getStatus())) {
            throw new BusinessException("Purchase order already received");
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("Cannot receive a cancelled purchase order");
        }
        for (PurchaseOrderItem item : order.getItems()) {
            inventoryService.addStock(item.getProduct().getId(), item.getLotNumber(),
                    item.getExpiryDate(), item.getQuantity());
        }
        order.setStatus("RECEIVED");
        return repo.save(order);
    }

    @Transactional
    public PurchaseOrder cancel(Long id) {
        PurchaseOrder order = findById(id);
        if ("RECEIVED".equals(order.getStatus())) {
            throw new BusinessException("Cannot cancel a received purchase order");
        }
        order.setStatus("CANCELLED");
        return repo.save(order);
    }

    @Transactional
    public void delete(Long id) {
        PurchaseOrder order = findById(id);
        if ("RECEIVED".equals(order.getStatus())) {
            throw new BusinessException("Cannot delete a received purchase order");
        }
        repo.delete(order);
    }
}
