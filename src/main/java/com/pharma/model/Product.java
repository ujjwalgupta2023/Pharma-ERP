package com.pharma.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Column(nullable = false)
    private String name;

    /** Stock Keeping Unit / internal article code. */
    @Column(unique = true)
    private String sku;

    /** Generic / active ingredient, e.g. Paracetamol. */
    private String genericName;

    /** Dosage form, e.g. Tablet, Syrup, Injection. */
    private String form;

    /** Strength, e.g. "500 mg". */
    private String strength;

    private String manufacturer;

    private String category;

    /** HSN / SAC code for GST, e.g. 30049099. */
    private String hsnCode;

    /** Pack description, e.g. "500ML", "10 Tablets". */
    private String pack;

    /** GST rate percentage applied on sale, e.g. 5, 12, 18. Split equally into SGST + CGST. */
    @PositiveOrZero
    @Column(nullable = false)
    private BigDecimal gstRate = BigDecimal.valueOf(5);

    /** Maximum retail price (printed on the bill). */
    @PositiveOrZero
    @Column(nullable = false)
    private BigDecimal mrp = BigDecimal.ZERO;

    /** Requires a prescription (Rx) vs over-the-counter (OTC). */
    private boolean prescriptionRequired = false;

    @PositiveOrZero
    @Column(nullable = false)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @PositiveOrZero
    @Column(nullable = false)
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    /** When total stock across batches falls below this, product is "low stock". */
    @PositiveOrZero
    private int reorderLevel = 10;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getHsnCode() { return hsnCode; }
    public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }

    public String getPack() { return pack; }
    public void setPack(String pack) { this.pack = pack; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }

    public boolean isPrescriptionRequired() { return prescriptionRequired; }
    public void setPrescriptionRequired(boolean prescriptionRequired) { this.prescriptionRequired = prescriptionRequired; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
}
