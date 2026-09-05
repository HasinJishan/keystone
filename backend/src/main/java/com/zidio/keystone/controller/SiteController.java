package com.zidio.keystone.controller;

import com.zidio.keystone.dto.SiteDto;
import com.zidio.keystone.service.SiteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers/{customerId}/sites")
@RequiredArgsConstructor
@Tag(name = "Sites")
public class SiteController {

    private final SiteService siteService;

    @GetMapping
    public List<SiteDto> byCustomer(@PathVariable Long customerId) {
        return siteService.byCustomer(customerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public SiteDto create(@PathVariable Long customerId, @Valid @RequestBody SiteDto dto) {
        SiteDto withCustomer = new SiteDto(dto.id(), customerId, dto.name(), dto.address());
        return siteService.create(withCustomer);
    }
}
