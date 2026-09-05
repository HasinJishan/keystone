package com.zidio.keystone.dto;

import java.util.Map;

public record DashboardSummary(
        Map<String, Long> countsByStatus,
        long overdueCount,
        double slaCompliancePercent,
        Map<String, Long> countByTechnician
) {}
