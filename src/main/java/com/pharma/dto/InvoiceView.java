package com.pharma.dto;

import com.pharma.model.CompanySettings;
import com.pharma.model.Customer;
import com.pharma.model.SalesOrderItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Everything the printable GST bill needs in one payload. */
public class InvoiceView {

    public CompanySettings company;
    public Customer customer;

    public String invoiceNumber;
    public LocalDate orderDate;
    public LocalDate dueDate;
    public String status;

    public List<SalesOrderItem> items;

    /** Per-GST-rate rollup for the class table at the bottom of the bill. */
    public List<GstClassRow> gstClasses;

    public BigDecimal subTotal = BigDecimal.ZERO;      // sum of taxable values
    public BigDecimal itemDiscount = BigDecimal.ZERO;  // sum of line discounts
    public BigDecimal totalSgst = BigDecimal.ZERO;
    public BigDecimal totalCgst = BigDecimal.ZERO;
    public BigDecimal totalGst = BigDecimal.ZERO;
    public BigDecimal roundOff = BigDecimal.ZERO;
    public BigDecimal grandTotal = BigDecimal.ZERO;
    public int itemCount;

    public static class GstClassRow {
        public BigDecimal rate;        // e.g. 5, 12
        public BigDecimal taxable;     // taxable value in this class
        public BigDecimal discount;
        public BigDecimal sgst;
        public BigDecimal cgst;
        public BigDecimal totalGst;

        public GstClassRow(BigDecimal rate) {
            this.rate = rate;
            this.taxable = BigDecimal.ZERO;
            this.discount = BigDecimal.ZERO;
            this.sgst = BigDecimal.ZERO;
            this.cgst = BigDecimal.ZERO;
            this.totalGst = BigDecimal.ZERO;
        }
    }
}
