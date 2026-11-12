package com.pharma.service;

import com.pharma.exception.NotFoundException;
import com.pharma.model.Product;
import com.pharma.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    public List<Product> findAll() {
        return repo.findAll();
    }

    public Product findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    public List<Product> search(String q) {
        return repo.findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCaseOrSkuContainingIgnoreCase(q, q, q);
    }

    public Product create(Product product) {
        product.setId(null);
        return repo.save(product);
    }

    public Product update(Long id, Product data) {
        Product p = findById(id);
        p.setName(data.getName());
        p.setSku(data.getSku());
        p.setGenericName(data.getGenericName());
        p.setForm(data.getForm());
        p.setStrength(data.getStrength());
        p.setManufacturer(data.getManufacturer());
        p.setCategory(data.getCategory());
        p.setHsnCode(data.getHsnCode());
        p.setPack(data.getPack());
        p.setGstRate(data.getGstRate() != null ? data.getGstRate() : p.getGstRate());
        p.setMrp(data.getMrp() != null ? data.getMrp() : p.getMrp());
        p.setPrescriptionRequired(data.isPrescriptionRequired());
        p.setPurchasePrice(data.getPurchasePrice());
        p.setSellingPrice(data.getSellingPrice());
        p.setReorderLevel(data.getReorderLevel());
        return repo.save(p);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Product not found: " + id);
        }
        repo.deleteById(id);
    }
}
