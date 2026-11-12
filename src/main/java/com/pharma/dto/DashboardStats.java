package com.pharma.dto;

import java.math.BigDecimal;

public class DashboardStats {
    public long totalProducts;
    public long totalCustomers;
    public long totalSuppliers;
    public int totalStockUnits;
    public BigDecimal inventoryValue = BigDecimal.ZERO;
    public long lowStockCount;
    public long expiredBatchCount;
    public long expiringSoonCount;
    public long openPurchaseOrders;
    public long confirmedSalesThisMonth;
    public BigDecimal salesRevenueThisMonth = BigDecimal.ZERO;
}
