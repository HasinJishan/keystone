package com.zidio.keystone.controller;

import com.zidio.keystone.dto.DashboardSummary;
import com.zidio.keystone.service.ReportingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports")
public class ReportController {

    private final ReportingService reportingService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','ADMIN')")
    public DashboardSummary summary() {
        return reportingService.summary();
    }
}
