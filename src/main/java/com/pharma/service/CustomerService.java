package com.pharma.service;

import com.pharma.exception.NotFoundException;
import com.pharma.model.Customer;
import com.pharma.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository repo;

    public CustomerService(CustomerRepository repo) {
        this.repo = repo;
    }

    public List<Customer> findAll() {
        return repo.findAll();
    }

    public Customer findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + id));
    }

    public Customer create(Customer customer) {
        customer.setId(null);
        return repo.save(customer);
    }

    public Customer update(Long id, Customer data) {
        Customer c = findById(id);
        c.setName(data.getName());
        c.setType(data.getType());
        c.setContactPerson(data.getContactPerson());
        c.setEmail(data.getEmail());
        c.setPhone(data.getPhone());
        c.setAddress(data.getAddress());
        return repo.save(c);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Customer not found: " + id);
        }
        repo.deleteById(id);
    }
}
