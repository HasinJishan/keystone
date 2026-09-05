package com.zidio.keystone.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "work_order_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkOrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_id", nullable = false)
    private Long workOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private WorkOrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private WorkOrderStatus toStatus;

    @Column(name = "changed_by")
    private Long changedBy;

    @Builder.Default
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    @Column(length = 500)
    private String note;
}
