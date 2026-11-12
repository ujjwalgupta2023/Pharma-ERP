package com.pharma.controller;

import com.pharma.model.CompanySettings;
import com.pharma.service.CompanySettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
@CrossOrigin
public class CompanySettingsController {

    private final CompanySettingsService service;

    public CompanySettingsController(CompanySettingsService service) {
        this.service = service;
    }

    @GetMapping
    public CompanySettings get() {
        return service.get();
    }

    @PutMapping
    public CompanySettings save(@RequestBody CompanySettings settings) {
        return service.save(settings);
    }
}
