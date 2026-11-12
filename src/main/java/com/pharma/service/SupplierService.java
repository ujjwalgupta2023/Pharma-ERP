package com.pharma.service;

import com.pharma.exception.NotFoundException;
import com.pharma.model.Supplier;
import com.pharma.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository repo;

    public SupplierService(SupplierRepository repo) {
        this.repo = repo;
    }

    public List<Supplier> findAll() {
        return repo.findAll();
    }

    public Supplier findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + id));
    }

    public Supplier create(Supplier supplier) {
        supplier.setId(null);
        return repo.save(supplier);
    }

    public Supplier update(Long id, Supplier data) {
        Supplier s = findById(id);
        s.setName(data.getName());
        s.setContactPerson(data.getContactPerson());
        s.setEmail(data.getEmail());
        s.setPhone(data.getPhone());
        s.setAddress(data.getAddress());
        s.setLicenseNumber(data.getLicenseNumber());
        return repo.save(s);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Supplier not found: " + id);
        }
        repo.deleteById(id);
    }
}
