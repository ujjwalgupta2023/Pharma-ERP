package com.pharma.service;

import com.pharma.dto.DashboardStats;
import com.pharma.dto.StockSummary;
import com.pharma.model.SalesOrder;
import com.pharma.repository.CustomerRepository;
import com.pharma.repository.ProductRepository;
import com.pharma.repository.PurchaseOrderRepository;
import com.pharma.repository.SalesOrderRepository;
import com.pharma.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final ProductRepository productRepo;
    private final CustomerRepository customerRepo;
    private final SupplierRepository supplierRepo;
    private final PurchaseOrderRepository purchaseOrderRepo;
    private final SalesOrderRepository salesOrderRepo;
    private final InventoryService inventoryService;

    public DashboardService(ProductRepository productRepo, CustomerRepository customerRepo,
                            SupplierRepository supplierRepo, PurchaseOrderRepository purchaseOrderRepo,
                            SalesOrderRepository salesOrderRepo, InventoryService inventoryService) {
        this.productRepo = productRepo;
        this.customerRepo = customerRepo;
        this.supplierRepo = supplierRepo;
        this.purchaseOrderRepo = purchaseOrderRepo;
        this.salesOrderRepo = salesOrderRepo;
        this.inventoryService = inventoryService;
    }

    public DashboardStats stats() {
        DashboardStats s = new DashboardStats();
        s.totalProducts = productRepo.count();
        s.totalCustomers = customerRepo.count();
        s.totalSuppliers = supplierRepo.count();

        List<StockSummary> summaries = inventoryService.stockSummary();
        s.totalStockUnits = summaries.stream().mapToInt(x -> x.totalQuantity).sum();
        s.inventoryValue = summaries.stream()
                .map(x -> x.stockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        s.lowStockCount = summaries.stream().filter(x -> x.lowStock).count();

        s.expiredBatchCount = inventoryService.expiredBatches().size();
        s.expiringSoonCount = inventoryService.expiringSoon(90).size();

        s.openPurchaseOrders = purchaseOrderRepo.findAll().stream()
                .filter(po -> "DRAFT".equals(po.getStatus()))
                .count();

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        List<SalesOrder> monthlySales = salesOrderRepo
                .findByStatusAndOrderDateGreaterThanEqual("CONFIRMED", monthStart);
        s.confirmedSalesThisMonth = monthlySales.size();
        s.salesRevenueThisMonth = monthlySales.stream()
                .map(SalesOrder::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return s;
    }
}
