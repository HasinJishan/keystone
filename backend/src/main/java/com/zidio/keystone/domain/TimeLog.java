package com.zidio.keystone.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "time_logs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TimeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_id", nullable = false)
    private Long workOrderId;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @Column(nullable = false)
    private Integer minutes;

    @Column(length = 500)
    private String note;

    @Builder.Default
    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt = Instant.now();
}
