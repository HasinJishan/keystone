package com.zidio.keystone.controller;

import com.zidio.keystone.domain.WorkOrderStatus;
import com.zidio.keystone.dto.*;
import com.zidio.keystone.service.WorkOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * The heart of the API. Every endpoint here re-checks role and ownership server-side inside
 * WorkOrderService - this controller stays thin and only wires HTTP to that service, per
 * Section 06.1 of the brief.
 */
@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@Tag(name = "Work Orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    public Page<WorkOrderResponse> search(@RequestParam(required = false) WorkOrderStatus status,
                                           @RequestParam(required = false) Long customerId,
                                           @RequestParam(required = false) Long assignedTo,
                                           @RequestParam(required = false) String q,
                                           Pageable pageable) {
        return workOrderService.search(status, customerId, assignedTo, q, pageable);
    }

    @GetMapping("/{id}")
    public WorkOrderResponse get(@PathVariable Long id) {
        return workOrderService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkOrderResponse create(@Valid @RequestBody CreateWorkOrderRequest req) {
        return workOrderService.create(req);
    }

    @PutMapping("/{id}")
    public WorkOrderResponse update(@PathVariable Long id, @Valid @RequestBody UpdateWorkOrderRequest req) {
        return workOrderService.update(id, req);
    }

    @PostMapping("/{id}/assign")
    public WorkOrderResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        return workOrderService.assign(id, req);
    }

    @PostMapping("/{id}/status")
    public WorkOrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest req) {
        return workOrderService.changeStatus(id, req);
    }

    @PostMapping("/{id}/parts")
    public WorkOrderResponse logParts(@PathVariable Long id, @Valid @RequestBody PartUsageRequest req) {
        return workOrderService.logPartUsage(id, req);
    }

    @PostMapping("/{id}/time")
    public WorkOrderResponse logTime(@PathVariable Long id, @Valid @RequestBody TimeLogRequest req) {
        return workOrderService.logTime(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        workOrderService.hardDelete(id);
    }
}
