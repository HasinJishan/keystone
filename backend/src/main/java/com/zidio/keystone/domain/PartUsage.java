package com.zidio.keystone.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "part_usage")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_id", nullable = false)
    private Long workOrderId;

    @Column(name = "part_id", nullable = false)
    private Long partId;

    @Column(name = "qty_used", nullable = false)
    private Integer qtyUsed;

    @Column(name = "logged_by")
    private Long loggedBy;

    @Builder.Default
    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt = Instant.now();
}
