package com.pharma.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "sales_order_items")
public class SalesOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sales_order_id")
    @JsonIgnore
    private SalesOrder salesOrder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    private int quantity;

    /** Free (bonus) units given, printed in the FREE column. Not stock-charged. */
    private int freeQuantity = 0;

    private BigDecimal unitPrice = BigDecimal.ZERO;

    // Snapshots taken at sale time so the printed bill stays accurate.
    private String hsnCode;
    private String pack;
    private String batchNumber;
    private String expiryDate;        // printed as MM/YY
    private BigDecimal mrp = BigDecimal.ZERO;

    /** Discount percentage on this line. */
    private BigDecimal discountPercent = BigDecimal.ZERO;

    /** GST rate percentage (split equally into SGST + CGST). */
    private BigDecimal gstRate = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SalesOrder getSalesOrder() { return salesOrder; }
    public void setSalesOrder(SalesOrder salesOrder) { this.salesOrder = salesOrder; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getFreeQuantity() { return freeQuantity; }
    public void setFreeQuantity(int freeQuantity) { this.freeQuantity = freeQuantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public String getHsnCode() { return hsnCode; }
    public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }

    public String getPack() { return pack; }
    public void setPack(String pack) { this.pack = pack; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }

    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    /** Gross = rate x qty, before discount and tax. */
    @Transient
    public BigDecimal getGross() {
        return nz(unitPrice).multiply(BigDecimal.valueOf(quantity));
    }

    /** Discount amount in rupees. */
    @Transient
    public BigDecimal getDiscountAmount() {
        return getGross().multiply(nz(discountPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** Taxable value after discount. */
    @Transient
    public BigDecimal getTaxable() {
        return getGross().subtract(getDiscountAmount()).setScale(2, RoundingMode.HALF_UP);
    }

    /** Total GST (SGST + CGST) in rupees. */
    @Transient
    public BigDecimal getGstAmount() {
        return getTaxable().multiply(nz(gstRate))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transient
    public BigDecimal getSgstAmount() {
        return getGstAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    @Transient
    public BigDecimal getCgstAmount() {
        return getGstAmount().subtract(getSgstAmount());
    }

    /** Line total shown in the AMOUNT column: taxable + GST. */
    @Transient
    public BigDecimal getLineTotal() {
        return getTaxable().add(getGstAmount());
    }
}
