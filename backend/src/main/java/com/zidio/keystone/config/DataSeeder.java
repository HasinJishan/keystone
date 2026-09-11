package com.zidio.keystone.config;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Seeds sample data on first startup, the same way taught in training: since we use
 * ddl-auto=update (Hibernate creates/updates tables from the entities) instead of Flyway,
 * there is no SQL migration step to carry seed data - so we insert it here in code,
 * through the repositories, exactly once. If users already exist, this does nothing,
 * so it's safe to leave running on every startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final PartRepository partRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Seed data already present - skipping.");
            return;
        }
        log.info("No users found - seeding sample data...");

        Customer meridian = customerRepository.save(Customer.builder()
                .name("Meridian Facilities Management")
                .contactEmail("ops@meridianfm.example")
                .contactPhone("+91-9000000001")
                .build());
        Customer blueHarbor = customerRepository.save(Customer.builder()
                .name("Blue Harbor Logistics")
                .contactEmail("facilities@blueharbor.example")
                .contactPhone("+91-9000000002")
                .build());

        Site towerA = siteRepository.save(Site.builder()
                .customerId(meridian.getId()).name("Meridian Tower A").address("12 MG Road, Coimbatore").build());
        Site towerB = siteRepository.save(Site.builder()
                .customerId(meridian.getId()).name("Meridian Tower B").address("45 Avinashi Road, Coimbatore").build());
        Site warehouse1 = siteRepository.save(Site.builder()
                .customerId(blueHarbor.getId()).name("Blue Harbor Warehouse 1").address("9 Port Street, Chennai").build());

        String pw = passwordEncoder.encode("Passw0rd!");

        User dispatcher = userRepository.save(User.builder()
                .name("Dana Dispatcher").email("dispatcher@keystone.dev")
                .passwordHash(pw).role(Role.DISPATCHER).active(true).build());
        User technician = userRepository.save(User.builder()
                .name("Tariq Technician").email("technician@keystone.dev")
                .passwordHash(pw).role(Role.TECHNICIAN).active(true).build());
        User manager = userRepository.save(User.builder()
                .name("Mona Manager").email("manager@keystone.dev")
                .passwordHash(pw).role(Role.MANAGER).active(true).build());
        userRepository.save(User.builder()
                .name("Cara Customer").email("customer@keystone.dev")
                .passwordHash(pw).role(Role.CUSTOMER).customerId(meridian.getId()).active(true).build());

        partRepository.save(Part.builder().name("Air Filter 20x20").sku("FLT-2020")
                .unitCost(new BigDecimal("12.50")).stockQty(40).build());
        partRepository.save(Part.builder().name("Refrigerant R410A (lb)").sku("REF-410A")
                .unitCost(new BigDecimal("18.00")).stockQty(25).build());
        partRepository.save(Part.builder().name("Circuit Breaker 20A").sku("ELE-CB20")
                .unitCost(new BigDecimal("9.75")).stockQty(30).build());
        partRepository.save(Part.builder().name("PVC Pipe Fitting 1in").sku("PLB-PVC1")
                .unitCost(new BigDecimal("3.20")).stockQty(100).build());

        WorkOrder wo1 = workOrderRepository.save(WorkOrder.builder()
                .code("WO-0001").title("HVAC unit not cooling").description("Rooftop AHU-2 blowing warm air.")
                .priority(Priority.HIGH).status(WorkOrderStatus.NEW)
                .customerId(meridian.getId()).siteId(towerA.getId())
                .createdBy(dispatcher.getId())
                .slaDueAt(Instant.now().plus(8, ChronoUnit.HOURS))
                .build());
        historyRepository.save(WorkOrderStatusHistory.builder()
                .workOrderId(wo1.getId()).toStatus(WorkOrderStatus.NEW)
                .changedBy(dispatcher.getId()).note("Request logged").build());

        WorkOrder wo2 = workOrderRepository.save(WorkOrder.builder()
                .code("WO-0002").title("Breaker tripping in server room").description("Panel B breaker 14 trips repeatedly.")
                .priority(Priority.CRITICAL).status(WorkOrderStatus.ASSIGNED)
                .customerId(meridian.getId()).siteId(towerB.getId())
                .assignedTo(technician.getId()).createdBy(dispatcher.getId())
                .slaDueAt(Instant.now().plus(4, ChronoUnit.HOURS))
                .build());
        historyRepository.save(WorkOrderStatusHistory.builder()
                .workOrderId(wo2.getId()).toStatus(WorkOrderStatus.NEW)
                .changedBy(dispatcher.getId()).note("Request logged").build());
        historyRepository.save(WorkOrderStatusHistory.builder()
                .workOrderId(wo2.getId()).fromStatus(WorkOrderStatus.NEW).toStatus(WorkOrderStatus.ASSIGNED)
                .changedBy(dispatcher.getId()).note("Assigned to Tariq").build());

        WorkOrder wo3 = workOrderRepository.save(WorkOrder.builder()
                .code("WO-0003").title("Leaking pipe under sink").description("Slow leak, staff placed a bucket.")
                .priority(Priority.MEDIUM).status(WorkOrderStatus.IN_PROGRESS)
                .customerId(blueHarbor.getId()).siteId(warehouse1.getId())
                .assignedTo(technician.getId()).createdBy(dispatcher.getId())
                .slaDueAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build());
        historyRepository.save(WorkOrderStatusHistory.builder()
                .workOrderId(wo3.getId()).toStatus(WorkOrderStatus.NEW)
                .changedBy(dispatcher.getId()).note("Request logged").build());
        historyRepository.save(WorkOrderStatusHistory.builder()
                .workOrderId(wo3.getId()).fromStatus(WorkOrderStatus.NEW).toStatus(WorkOrderStatus.ASSIGNED)
                .changedBy(dispatcher.getId()).note("Assigned to Tariq").build());
        historyRepository.save(WorkOrderStatusHistory.builder()
                .workOrderId(wo3.getId()).fromStatus(WorkOrderStatus.ASSIGNED).toStatus(WorkOrderStatus.IN_PROGRESS)
                .changedBy(technician.getId()).note("Started work").build());

        log.info("Seed data created: 2 customers, 3 sites, 4 users, 4 parts, 3 work orders.");
    }
}
