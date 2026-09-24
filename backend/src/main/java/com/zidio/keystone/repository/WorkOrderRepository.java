package com.zidio.keystone.repository;

import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    Optional<WorkOrder> findByCode(String code);

    long countByStatus(WorkOrderStatus status);

    Page<WorkOrder> findByCustomerId(Long customerId, Pageable pageable);

    Page<WorkOrder> findByAssignedTo(Long technicianId, Pageable pageable);

    /**
     * The :q parameter is explicitly cast to string. Without this, PostgreSQL cannot
     * infer the type of a null parameter used inside concat()/lower() and defaults to
     * bytea, throwing "function lower(bytea) does not exist" - this only shows up
     * when no search term is typed (:q is null), which is exactly the normal case of
     * just opening the board or portal. The cast fixes it for both the null case and
     * the real-search case.
     */
    @Query("""
           select w from WorkOrder w
           where (:status is null or w.status = :status)
             and (:customerId is null or w.customerId = :customerId)
             and (:assignedTo is null or w.assignedTo = :assignedTo)
             and (:q is null or lower(w.title) like lower(concat('%', cast(:q as string), '%'))
                            or lower(w.code) like lower(concat('%', cast(:q as string), '%')))
           """)
    Page<WorkOrder> search(@Param("status") WorkOrderStatus status,
                            @Param("customerId") Long customerId,
                            @Param("assignedTo") Long assignedTo,
                            @Param("q") String q,
                            Pageable pageable);

    @Query("""
           select w from WorkOrder w
           where w.status not in ('CLOSED','CANCELLED')
             and w.slaDueAt < :now
             and w.slaBreached = false
           """)
    List<WorkOrder> findNewlyBreached(@Param("now") Instant now);

    @Query("select count(w) from WorkOrder w where w.status not in ('CLOSED','CANCELLED') and w.slaDueAt < :now")
    long countOverdue(@Param("now") Instant now);
}
