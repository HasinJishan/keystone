package com.zidio.keystone.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The governed work-order lifecycle. This is the single source of truth for which
 * transitions are legal - the service layer consults this enum before writing any change.
 */
public enum WorkOrderStatus {
    NEW, ASSIGNED, IN_PROGRESS, ON_HOLD, COMPLETED, CLOSED, CANCELLED;

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED = Map.of(
            NEW, EnumSet.of(ASSIGNED, CANCELLED),
            ASSIGNED, EnumSet.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, EnumSet.of(ON_HOLD, COMPLETED),
            ON_HOLD, EnumSet.of(IN_PROGRESS),
            COMPLETED, EnumSet.of(CLOSED, IN_PROGRESS),
            CLOSED, EnumSet.noneOf(WorkOrderStatus.class),
            CANCELLED, EnumSet.noneOf(WorkOrderStatus.class)
    );

    public boolean canTransitionTo(WorkOrderStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }
}
