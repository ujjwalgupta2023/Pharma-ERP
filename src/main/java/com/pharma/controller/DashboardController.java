package com.pharma.controller;

import com.pharma.dto.DashboardStats;
import com.pharma.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public DashboardStats stats() {
        return service.stats();
    }
}
