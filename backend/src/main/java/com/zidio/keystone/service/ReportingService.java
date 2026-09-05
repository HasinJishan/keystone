package com.zidio.keystone.service;

import com.zidio.keystone.domain.User;
import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatus;
import com.zidio.keystone.dto.DashboardSummary;
import com.zidio.keystone.repository.UserRepository;
import com.zidio.keystone.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Backs the manager dashboard (F8): status mix, overdue count, SLA compliance, per-technician load. */
@Service
@RequiredArgsConstructor
public class ReportingService {

    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;

    public DashboardSummary summary() {
        List<WorkOrder> all = workOrderRepository.findAll();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (WorkOrderStatus s : WorkOrderStatus.values()) {
            byStatus.put(s.name(), all.stream().filter(w -> w.getStatus() == s).count());
        }

        long overdue = workOrderRepository.countOverdue(Instant.now());

        long closedCount = all.stream().filter(w -> w.getStatus() == WorkOrderStatus.CLOSED).count();
        long closedOnTime = all.stream()
                .filter(w -> w.getStatus() == WorkOrderStatus.CLOSED && !w.isSlaBreached())
                .count();
        double compliance = closedCount == 0 ? 100.0 : (closedOnTime * 100.0) / closedCount;

        Map<String, Long> byTechnician = all.stream()
                .filter(w -> w.getAssignedTo() != null)
                .collect(Collectors.groupingBy(
                        w -> userRepository.findById(w.getAssignedTo()).map(User::getName).orElse("Unknown"),
                        Collectors.counting()));

        return new DashboardSummary(byStatus, overdue, Math.round(compliance * 10.0) / 10.0, byTechnician);
    }
}
