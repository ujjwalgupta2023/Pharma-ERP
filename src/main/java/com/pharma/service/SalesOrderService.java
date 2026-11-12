package com.pharma.service;

import com.pharma.dto.InvoiceView;
import com.pharma.exception.BusinessException;
import com.pharma.exception.NotFoundException;
import com.pharma.model.*;
import com.pharma.repository.CustomerRepository;
import com.pharma.repository.ProductRepository;
import com.pharma.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesOrderService {

    private final SalesOrderRepository repo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;
    private final InventoryService inventoryService;
    private final CompanySettingsService companySettingsService;

    public SalesOrderService(SalesOrderRepository repo, CustomerRepository customerRepo,
                             ProductRepository productRepo, InventoryService inventoryService,
                             CompanySettingsService companySettingsService) {
        this.repo = repo;
        this.customerRepo = customerRepo;
        this.productRepo = productRepo;
        this.inventoryService = inventoryService;
        this.companySettingsService = companySettingsService;
    }

    /** Assemble the full printable bill for a sales order. */
    @Transactional(readOnly = true)
    public InvoiceView invoice(Long id) {
        SalesOrder order = findById(id);
        InvoiceView v = new InvoiceView();
        v.company = companySettingsService.get();
        v.customer = order.getCustomer();
        v.invoiceNumber = order.getInvoiceNumber() != null ? order.getInvoiceNumber() : ("#" + order.getId());
        v.orderDate = order.getOrderDate();
        v.dueDate = order.getOrderDate();
        v.status = order.getStatus();
        v.items = order.getItems();
        v.itemCount = order.getItems().size();

        Map<String, InvoiceView.GstClassRow> classes = new LinkedHashMap<>();
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal itemDiscount = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;

        for (SalesOrderItem item : order.getItems()) {
            BigDecimal taxable = item.getTaxable();
            subTotal = subTotal.add(taxable);
            itemDiscount = itemDiscount.add(item.getDiscountAmount());
            sgst = sgst.add(item.getSgstAmount());
            cgst = cgst.add(item.getCgstAmount());

            BigDecimal rate = item.getGstRate() == null ? BigDecimal.ZERO : item.getGstRate();
            String key = rate.stripTrailingZeros().toPlainString();
            InvoiceView.GstClassRow row = classes.computeIfAbsent(key, k -> new InvoiceView.GstClassRow(rate));
            row.taxable = row.taxable.add(taxable);
            row.discount = row.discount.add(item.getDiscountAmount());
            row.sgst = row.sgst.add(item.getSgstAmount());
            row.cgst = row.cgst.add(item.getCgstAmount());
            row.totalGst = row.totalGst.add(item.getGstAmount());
        }

        v.gstClasses = new ArrayList<>(classes.values());
        v.subTotal = subTotal.setScale(2, RoundingMode.HALF_UP);
        v.itemDiscount = itemDiscount.setScale(2, RoundingMode.HALF_UP);
        v.totalSgst = sgst.setScale(2, RoundingMode.HALF_UP);
        v.totalCgst = cgst.setScale(2, RoundingMode.HALF_UP);
        v.totalGst = sgst.add(cgst).setScale(2, RoundingMode.HALF_UP);

        BigDecimal beforeRound = v.subTotal.add(v.totalGst);
        BigDecimal rounded = beforeRound.setScale(0, RoundingMode.HALF_UP);
        v.roundOff = rounded.subtract(beforeRound).setScale(2, RoundingMode.HALF_UP);
        v.grandTotal = rounded.setScale(2, RoundingMode.HALF_UP);
        return v;
    }

    @Transactional(readOnly = true)
    public List<SalesOrder> findAll() {
        List<SalesOrder> orders = repo.findAllByOrderByOrderDateDesc();
        orders.forEach(o -> o.getItems().size()); // initialize within session
        return orders;
    }

    @Transactional(readOnly = true)
    public SalesOrder findById(Long id) {
        SalesOrder order = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Sales order not found: " + id));
        order.getItems().size(); // initialize within session
        return order;
    }

    @Transactional
    public SalesOrder create(SalesOrder order) {
        order.setId(null);
        order.setStatus("DRAFT");

        if (order.getCustomer() != null && order.getCustomer().getId() != null) {
            Customer customer = customerRepo.findById(order.getCustomer().getId())
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + order.getCustomer().getId()));
            order.setCustomer(customer);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (SalesOrderItem item : order.getItems()) {
            Product product = productRepo.findById(item.getProduct().getId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + item.getProduct().getId()));
            item.setProduct(product);
            item.setSalesOrder(order);
            // Default unit price from product selling price if not supplied.
            if (item.getUnitPrice() == null || item.getUnitPrice().signum() == 0) {
                item.setUnitPrice(product.getSellingPrice());
            }
            // Snapshot GST / bill fields from the product when not supplied on the line.
            if (item.getHsnCode() == null) item.setHsnCode(product.getHsnCode());
            if (item.getPack() == null) item.setPack(product.getPack());
            if (item.getMrp() == null || item.getMrp().signum() == 0) item.setMrp(product.getMrp());
            if (item.getGstRate() == null || item.getGstRate().signum() == 0) item.setGstRate(product.getGstRate());
            if (item.getDiscountPercent() == null) item.setDiscountPercent(BigDecimal.ZERO);
            total = total.add(item.getLineTotal());
        }
        order.setTotal(total.setScale(2, java.math.RoundingMode.HALF_UP));
        SalesOrder saved = repo.save(order);
        if (saved.getInvoiceNumber() == null || saved.getInvoiceNumber().isBlank()) {
            saved.setInvoiceNumber("INV-" + String.format("%05d", saved.getId()));
            saved = repo.save(saved);
        }
        return saved;
    }

    /** Confirm the sale, deducting stock via FEFO. Validates stock up front. */
    @Transactional
    public SalesOrder confirm(Long id) {
        SalesOrder order = findById(id);
        if ("CONFIRMED".equals(order.getStatus())) {
            throw new BusinessException("Sales order already confirmed");
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("Cannot confirm a cancelled sales order");
        }
        // Validate all lines before deducting any stock.
        for (SalesOrderItem item : order.getItems()) {
            int available = inventoryService.totalQuantity(item.getProduct().getId());
            if (available < item.getQuantity()) {
                throw new BusinessException("Insufficient stock for " + item.getProduct().getName()
                        + ": requested " + item.getQuantity() + ", available " + available);
            }
        }
        for (SalesOrderItem item : order.getItems()) {
            inventoryService.removeStock(item.getProduct().getId(), item.getQuantity());
        }
        order.setStatus("CONFIRMED");
        return repo.save(order);
    }

    @Transactional
    public SalesOrder cancel(Long id) {
        SalesOrder order = findById(id);
        if ("CONFIRMED".equals(order.getStatus())) {
            throw new BusinessException("Cannot cancel a confirmed sales order (stock already shipped)");
        }
        order.setStatus("CANCELLED");
        return repo.save(order);
    }

    @Transactional
    public void delete(Long id) {
        SalesOrder order = findById(id);
        if ("CONFIRMED".equals(order.getStatus())) {
            throw new BusinessException("Cannot delete a confirmed sales order");
        }
        repo.delete(order);
    }
}
