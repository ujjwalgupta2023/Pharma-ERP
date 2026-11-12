package com.pharma.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Aggregated stock information for a single product across its batches. */
public class StockSummary {
    public Long productId;
    public String name;
    public String sku;
    public String form;
    public String strength;
    public int totalQuantity;
    public int reorderLevel;
    public boolean lowStock;
    public LocalDate earliestExpiry;
    public BigDecimal stockValue;

    public StockSummary(Long productId, String name, String sku, String form, String strength,
                        int totalQuantity, int reorderLevel, LocalDate earliestExpiry, BigDecimal stockValue) {
        this.productId = productId;
        this.name = name;
        this.sku = sku;
        this.form = form;
        this.strength = strength;
        this.totalQuantity = totalQuantity;
        this.reorderLevel = reorderLevel;
        this.lowStock = totalQuantity <= reorderLevel;
        this.earliestExpiry = earliestExpiry;
        this.stockValue = stockValue;
    }
}
