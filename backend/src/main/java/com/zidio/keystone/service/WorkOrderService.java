package com.zidio.keystone.service;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.dto.*;
import com.zidio.keystone.exception.ForbiddenOperationException;
import com.zidio.keystone.exception.IllegalTransitionException;
import com.zidio.keystone.exception.InsufficientStockException;
import com.zidio.keystone.exception.ResourceNotFoundException;
import com.zidio.keystone.repository.*;
import com.zidio.keystone.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Owns the work-order lifecycle end to end. This is deliberately the "thick" service in the
 * platform: every rule in Section 07/08/09 of the brief (guarded transitions, role checks,
 * transactional parts/stock, audit trail) is enforced here - never trusted from the client.
 */
@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final PartUsageRepository partUsageRepository;
    private final TimeLogRepository timeLogRepository;
    private final PartRepository partRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    @Value("${app.sla.critical-hours}") private long criticalHours;
    @Value("${app.sla.high-hours}") private long highHours;
    @Value("${app.sla.medium-hours}") private long mediumHours;
    @Value("${app.sla.low-hours}") private long lowHours;

    // ---------- Create ----------

    @Transactional
    public WorkOrderResponse create(CreateWorkOrderRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + req.customerId()));
        Site site = siteRepository.findById(req.siteId())
                .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + req.siteId()));
        if (!site.getCustomerId().equals(customer.getId())) {
            throw new IllegalArgumentException("Site does not belong to the given customer");
        }

        // Customers can only raise requests for their own organisation (F9.1)
        if ("CUSTOMER".equals(currentUser.role()) && !customer.getId().equals(currentUser.customerId())) {
            throw new ForbiddenOperationException("Customers may only raise requests for their own sites");
        }

        WorkOrder wo = WorkOrder.builder()
                .code(nextCode())
                .title(req.title())
                .description(req.description())
                .priority(req.priority())
                .status(WorkOrderStatus.NEW)
                .customerId(req.customerId())
                .siteId(req.siteId())
                .createdBy(currentUser.id())
                .slaDueAt(Instant.now().plus(slaHoursFor(req.priority()), ChronoUnit.HOURS))
                .build();

        wo = workOrderRepository.save(wo);
        writeHistory(wo.getId(), null, WorkOrderStatus.NEW, "Work order raised");

        return toResponse(wo);
    }

    private long slaHoursFor(Priority priority) {
        return switch (priority) {
            case CRITICAL -> criticalHours;
            case HIGH -> highHours;
            case MEDIUM -> mediumHours;
            case LOW -> lowHours;
        };
    }

    private synchronized String nextCode() {
        long n = workOrderRepository.count() + 1;
        String candidate;
        do {
            candidate = String.format("WO-%04d", n);
            n++;
        } while (workOrderRepository.findByCode(candidate).isPresent());
        return candidate;
    }

    // ---------- Read ----------

    public WorkOrderResponse get(Long id) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + id));
        assertReadAccess(wo);
        return toResponse(wo);
    }

    public Page<WorkOrderResponse> search(WorkOrderStatus status, Long customerId, Long assignedTo,
                                           String q, Pageable pageable) {
        // Role-scoped visibility (Section 03/08): customers and technicians never see others' data.
        String role = currentUser.role();
        if ("CUSTOMER".equals(role)) {
            customerId = currentUser.customerId();
        } else if ("TECHNICIAN".equals(role)) {
            assignedTo = currentUser.id();
        }
        return workOrderRepository.search(status, customerId, assignedTo, q, pageable)
                .map(this::toResponse);
    }

    private void assertReadAccess(WorkOrder wo) {
        String role = currentUser.role();
        if ("CUSTOMER".equals(role) && !wo.getCustomerId().equals(currentUser.customerId())) {
            throw new ForbiddenOperationException("Not your organisation's work order");
        }
        if ("TECHNICIAN".equals(role) && (wo.getAssignedTo() == null || !wo.getAssignedTo().equals(currentUser.id()))) {
            throw new ForbiddenOperationException("This work order is not assigned to you");
        }
    }

    // ---------- Update (open jobs only) ----------

    @Transactional
    public WorkOrderResponse update(Long id, UpdateWorkOrderRequest req) {
        WorkOrder wo = getForWrite(id);
        if (wo.getStatus().isTerminal()) {
            throw new IllegalTransitionException("Cannot edit a " + wo.getStatus() + " work order");
        }
        wo.setTitle(req.title());
        wo.setDescription(req.description());
        wo.setPriority(req.priority());
        return toResponse(wo);
    }

    // ---------- Dispatch / assignment (F4) ----------

    @Transactional
    public WorkOrderResponse assign(Long id, AssignRequest req) {
        requireRole("DISPATCHER", "MANAGER");
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + id));

        User technician = userRepository.findById(req.technicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + req.technicianId()));
        if (technician.getRole() != Role.TECHNICIAN) {
            throw new IllegalArgumentException("User " + req.technicianId() + " is not a technician");
        }

        if (wo.getStatus() == WorkOrderStatus.NEW) {
            transition(wo, WorkOrderStatus.ASSIGNED, "Assigned to " + technician.getName());
        } else if (wo.getStatus().isTerminal()) {
            throw new IllegalTransitionException("Cannot assign a " + wo.getStatus() + " work order");
        }
        // reassignment allowed while open (F4.3), without forcing a status change if already ASSIGNED+
        wo.setAssignedTo(technician.getId());
        return toResponse(wo);
    }

    // ---------- Status transitions (Section 07) ----------

    @Transactional
    public WorkOrderResponse changeStatus(Long id, StatusChangeRequest req) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + id));

        assertTransitionRole(wo, req.toStatus());

        if (!wo.getStatus().canTransitionTo(req.toStatus())) {
            throw new IllegalTransitionException(
                    "Cannot move from " + wo.getStatus() + " to " + req.toStatus());
        }

        transition(wo, req.toStatus(), req.note());

        if (req.toStatus() == WorkOrderStatus.COMPLETED) {
            wo.setCompletedAt(Instant.now());
        }
        if (req.toStatus() == WorkOrderStatus.CLOSED) {
            wo.setClosedAt(Instant.now());
        }
        return toResponse(wo);
    }

    /** Role gate per transition, per Section 07.2: e.g. only the assigned technician can start;
     *  only a manager can close. */
    private void assertTransitionRole(WorkOrder wo, WorkOrderStatus target) {
        String role = currentUser.role();

        boolean isAssignedTechnician = "TECHNICIAN".equals(role)
                && wo.getAssignedTo() != null && wo.getAssignedTo().equals(currentUser.id());

        switch (target) {
            case IN_PROGRESS, ON_HOLD -> {
                if (!(isAssignedTechnician || "MANAGER".equals(role))) {
                    throw new ForbiddenOperationException("Only the assigned technician can update this job's progress");
                }
            }
            case COMPLETED -> {
                if (!(isAssignedTechnician || "MANAGER".equals(role))) {
                    throw new ForbiddenOperationException("Only the assigned technician can complete this job");
                }
            }
            case CLOSED -> {
                if (!"MANAGER".equals(role)) {
                    throw new ForbiddenOperationException("Only a manager can close a work order");
                }
            }
            case CANCELLED -> {
                if (!("DISPATCHER".equals(role) || "MANAGER".equals(role))) {
                    throw new ForbiddenOperationException("Only a dispatcher or manager can cancel a work order");
                }
            }
            default -> { /* ASSIGNED handled via /assign endpoint */ }
        }
    }

    private void transition(WorkOrder wo, WorkOrderStatus to, String note) {
        WorkOrderStatus from = wo.getStatus();
        wo.setStatus(to);
        writeHistory(wo.getId(), from, to, note);
    }

    private void writeHistory(Long workOrderId, WorkOrderStatus from, WorkOrderStatus to, String note) {
        historyRepository.save(WorkOrderStatusHistory.builder()
                .workOrderId(workOrderId)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(currentUser.id())
                .note(note)
                .build());
    }

    // ---------- Parts usage (F6) - transactional stock decrement ----------

    @Transactional
    public WorkOrderResponse logPartUsage(Long id, PartUsageRequest req) {
        WorkOrder wo = getForWrite(id);
        assertAssignedTechnicianOrManager(wo);

        Part part = partRepository.findById(req.partId())
                .orElseThrow(() -> new ResourceNotFoundException("Part not found: " + req.partId()));

        if (part.getStockQty() < req.qtyUsed()) {
            throw new InsufficientStockException(
                    "Not enough stock for " + part.getName() + " (have " + part.getStockQty() + ", need " + req.qtyUsed() + ")");
        }

        // Same transaction: decrement stock AND record usage together, or neither happens.
        part.setStockQty(part.getStockQty() - req.qtyUsed());
        partRepository.save(part);

        partUsageRepository.save(PartUsage.builder()
                .workOrderId(id)
                .partId(part.getId())
                .qtyUsed(req.qtyUsed())
                .loggedBy(currentUser.id())
                .build());

        return toResponse(wo);
    }

    // ---------- Time logging (F6) ----------

    @Transactional
    public WorkOrderResponse logTime(Long id, TimeLogRequest req) {
        WorkOrder wo = getForWrite(id);
        assertAssignedTechnicianOrManager(wo);

        timeLogRepository.save(TimeLog.builder()
                .workOrderId(id)
                .technicianId(currentUser.id())
                .minutes(req.minutes())
                .note(req.note())
                .build());

        return toResponse(wo);
    }

    private void assertAssignedTechnicianOrManager(WorkOrder wo) {
        String role = currentUser.role();
        boolean isAssignedTechnician = "TECHNICIAN".equals(role)
                && wo.getAssignedTo() != null && wo.getAssignedTo().equals(currentUser.id());
        if (!(isAssignedTechnician || "MANAGER".equals(role) || "DISPATCHER".equals(role))) {
            throw new ForbiddenOperationException("Only the assigned technician can log parts/time on this job");
        }
    }

    private WorkOrder getForWrite(Long id) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + id));
        assertReadAccess(wo);
        return wo;
    }

    private void requireRole(String... roles) {
        String role = currentUser.role();
        for (String r : roles) {
            if (r.equals(role)) return;
        }
        throw new ForbiddenOperationException("This action requires role: " + String.join(" or ", roles));
    }

    // ---------- Mapping ----------

    private WorkOrderResponse toResponse(WorkOrder wo) {
        Customer customer = customerRepository.findById(wo.getCustomerId()).orElse(null);
        Site site = siteRepository.findById(wo.getSiteId()).orElse(null);
        User assignee = wo.getAssignedTo() != null ? userRepository.findById(wo.getAssignedTo()).orElse(null) : null;

        List<WorkOrderResponse.StatusHistoryEntry> history = historyRepository
                .findByWorkOrderIdOrderByChangedAtAsc(wo.getId()).stream()
                .map(h -> new WorkOrderResponse.StatusHistoryEntry(
                        h.getFromStatus() != null ? h.getFromStatus().name() : null,
                        h.getToStatus().name(),
                        nameOf(h.getChangedBy()),
                        h.getChangedAt(),
                        h.getNote()))
                .toList();

        List<WorkOrderResponse.PartUsageEntry> parts = partUsageRepository.findByWorkOrderId(wo.getId()).stream()
                .map(pu -> new WorkOrderResponse.PartUsageEntry(
                        partRepository.findById(pu.getPartId()).map(Part::getName).orElse("Unknown part"),
                        pu.getQtyUsed(), pu.getLoggedAt()))
                .toList();

        List<WorkOrderResponse.TimeLogEntry> times = timeLogRepository.findByWorkOrderId(wo.getId()).stream()
                .map(t -> new WorkOrderResponse.TimeLogEntry(nameOf(t.getTechnicianId()), t.getMinutes(), t.getNote(), t.getLoggedAt()))
                .toList();

        return new WorkOrderResponse(
                wo.getId(), wo.getCode(), wo.getTitle(), wo.getDescription(), wo.getPriority(), wo.getStatus(),
                wo.getCustomerId(), customer != null ? customer.getName() : null,
                wo.getSiteId(), site != null ? site.getName() : null,
                wo.getAssignedTo(), assignee != null ? assignee.getName() : null,
                wo.getSlaDueAt(), wo.isSlaBreached(),
                wo.getCreatedAt(), wo.getUpdatedAt(), wo.getCompletedAt(), wo.getClosedAt(),
                history, parts, times
        );
    }

    private String nameOf(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(User::getName).orElse("Unknown");
    }
}
