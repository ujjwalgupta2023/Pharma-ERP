package com.pharma.config;

import com.pharma.model.*;
import com.pharma.repository.*;
import com.pharma.service.InventoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Seeds a small realistic dataset on first run (when the DB is empty). */
@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepo;
    private final SupplierRepository supplierRepo;
    private final CustomerRepository customerRepo;
    private final InventoryService inventoryService;

    public DataSeeder(ProductRepository productRepo, SupplierRepository supplierRepo,
                      CustomerRepository customerRepo, InventoryService inventoryService) {
        this.productRepo = productRepo;
        this.supplierRepo = supplierRepo;
        this.customerRepo = customerRepo;
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(String... args) {
        if (productRepo.count() > 0) {
            return; // already seeded
        }

        Product p1 = product("D-10 (DEXTOSE) (FFS)", "30049099", "Dextrose", "Infusion",
                "500ML", "Acme Pharma", "IV Fluids", false, "14.00", "18.48", "37.38", "5", 100);
        Product p2 = product("AMCIP FFS", "30042013", "Amikacin", "Infusion",
                "100ML", "BioCure Labs", "Antibiotic", true, "9.00", "11.81", "21.00", "5", 50);
        Product p3 = product("Cough Syrup", "30049091", "Dextromethorphan", "Syrup",
                "100ML", "Wellness Inc", "Respiratory", false, "10.00", "13.70", "24.00", "12", 40);
        Product p4 = product("Insulin Glargine", "30043100", "Insulin Glargine", "Injection",
                "100 IU/ml", "Nordic Biotech", "Antidiabetic", true, "28.00", "45.00", "60.00", "5", 20);
        Product p5 = product("Vitamin C 1000mg", "21069099", "Ascorbic Acid", "Tablet",
                "10 Tab", "HealthPlus", "Supplement", false, "8.00", "12.00", "18.00", "18", 80);

        supplier("Acme Pharma Distribution", "Ravi Kumar", "orders@acmepharma.com",
                "+49 170 1234567", "Industriestr. 5, Hannover", "DL-DE-1001");
        supplier("BioCure Wholesale", "Anna Weber", "sales@biocure.com",
                "+49 171 9876543", "Hafenweg 12, Hamburg", "DL-DE-2002");

        customer("City Pharmacy", "PHARMACY", "M. Schneider", "buy@citypharmacy.de",
                "+49 511 220033", "Marktplatz 1, Hannover");
        customer("General Hospital", "HOSPITAL", "Dr. Fischer", "procurement@genhosp.de",
                "+49 511 445566", "Klinikstr. 20, Hannover");
        customer("MediDistro GmbH", "DISTRIBUTOR", "T. Lang", "orders@medidistro.de",
                "+49 511 778899", "Logistikpark 7, Laatzen");

        // Seed stock as batches with varied expiry dates.
        inventoryService.addStock(p1.getId(), "PARA-A23", LocalDate.now().plusMonths(18), 500);
        inventoryService.addStock(p1.getId(), "PARA-A24", LocalDate.now().plusMonths(6), 150);
        inventoryService.addStock(p2.getId(), "AMOX-B11", LocalDate.now().plusMonths(10), 30); // low stock
        inventoryService.addStock(p3.getId(), "COUGH-C01", LocalDate.now().plusMonths(2), 60); // expiring soon
        inventoryService.addStock(p4.getId(), "INS-D09", LocalDate.now().plusMonths(12), 25);
        inventoryService.addStock(p5.getId(), "VITC-E55", LocalDate.now().minusMonths(1), 40); // expired
        inventoryService.addStock(p5.getId(), "VITC-E56", LocalDate.now().plusMonths(24), 200);
    }

    private Product product(String name, String hsn, String generic, String form, String pack,
                            String manufacturer, String category, boolean rx,
                            String purchase, String selling, String mrp, String gstRate, int reorder) {
        Product p = new Product();
        p.setName(name);
        p.setSku(hsn);
        p.setHsnCode(hsn);
        p.setGenericName(generic);
        p.setForm(form);
        p.setPack(pack);
        p.setManufacturer(manufacturer);
        p.setCategory(category);
        p.setPrescriptionRequired(rx);
        p.setPurchasePrice(new BigDecimal(purchase));
        p.setSellingPrice(new BigDecimal(selling));
        p.setMrp(new BigDecimal(mrp));
        p.setGstRate(new BigDecimal(gstRate));
        p.setReorderLevel(reorder);
        return productRepo.save(p);
    }

    private void supplier(String name, String contact, String email, String phone,
                          String address, String license) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setContactPerson(contact);
        s.setEmail(email);
        s.setPhone(phone);
        s.setAddress(address);
        s.setLicenseNumber(license);
        supplierRepo.save(s);
    }

    private void customer(String name, String type, String contact, String email,
                          String phone, String address) {
        Customer c = new Customer();
        c.setName(name);
        c.setType(type);
        c.setContactPerson(contact);
        c.setEmail(email);
        c.setPhone(phone);
        c.setAddress(address);
        customerRepo.save(c);
    }
}
