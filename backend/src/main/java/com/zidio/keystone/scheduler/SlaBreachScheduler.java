package com.zidio.keystone.scheduler;

import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Runs periodically to flag work orders that have crossed their SLA due date (F7.2/F7.3).
 * A real deployment would push an email/Slack alert here; for now it marks the order breached
 * so the dashboard and board can surface it, and logs it for anyone watching the console.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlaBreachScheduler {

    private final WorkOrderRepository workOrderRepository;

    @Scheduled(fixedRate = 5 * 60 * 1000) // every 5 minutes
    @Transactional
    public void checkForBreaches() {
        List<WorkOrder> newlyBreached = workOrderRepository.findNewlyBreached(Instant.now());
        for (WorkOrder wo : newlyBreached) {
            wo.setSlaBreached(true);
            log.warn("SLA BREACH: work order {} ({}) passed its due date of {}",
                    wo.getCode(), wo.getTitle(), wo.getSlaDueAt());
            // Notification hook: wire an email/Slack send here using spring-boot-starter-mail.
        }
        if (!newlyBreached.isEmpty()) {
            workOrderRepository.saveAll(newlyBreached);
        }
    }
}
