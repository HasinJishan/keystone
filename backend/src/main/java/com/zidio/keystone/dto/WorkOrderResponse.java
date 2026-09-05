package com.zidio.keystone.dto;

import com.zidio.keystone.domain.Priority;
import com.zidio.keystone.domain.WorkOrderStatus;

import java.time.Instant;
import java.util.List;

public record WorkOrderResponse(
        Long id,
        String code,
        String title,
        String description,
        Priority priority,
        WorkOrderStatus status,
        Long customerId,
        String customerName,
        Long siteId,
        String siteName,
        Long assignedTo,
        String assignedToName,
        Instant slaDueAt,
        boolean slaBreached,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        Instant closedAt,
        List<StatusHistoryEntry> history,
        List<PartUsageEntry> partsUsed,
        List<TimeLogEntry> timeLogs
) {
    public record StatusHistoryEntry(String fromStatus, String toStatus, String changedByName, Instant changedAt, String note) {}
    public record PartUsageEntry(String partName, int qtyUsed, Instant loggedAt) {}
    public record TimeLogEntry(String technicianName, int minutes, String note, Instant loggedAt) {}
}
