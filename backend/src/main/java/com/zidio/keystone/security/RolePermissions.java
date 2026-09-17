package com.zidio.keystone.security;

import com.zidio.keystone.domain.Permission;
import com.zidio.keystone.domain.Role;

import java.util.*;

import static com.zidio.keystone.domain.Permission.*;
import static com.zidio.keystone.domain.Role.*;

/**
 * The single source of truth for "which role gets which permissions" - built as an
 * EnumMap of Role to a List of Permission, exactly as taught: a List (backed by
 * ArrayList) rather than a Set so the permissions stay in the order they were granted,
 * and one static map built once rather than scattering role checks across every
 * service method. Controllers/services ask this class, not each other, whether a
 * role is allowed to do something.
 */
public final class RolePermissions {

    private static final Map<Role, List<Permission>> ROLE_PERMISSIONS = new EnumMap<>(Role.class);

    static {
        ROLE_PERMISSIONS.put(ADMIN, new ArrayList<>(List.of(
                LOGIN, LOGOUT,
                CREATE_CUSTOMER, UPDATE_CUSTOMER, DELETE_CUSTOMER, VIEW_CUSTOMER,
                CREATE_USER, UPDATE_USER, DELETE_USER, VIEW_USER,
                CREATE_SITE, UPDATE_SITE, VIEW_SITE,
                CREATE_WORK_ORDER, UPDATE_WORK_ORDER, VIEW_WORK_ORDER,
                ASSIGN_WORK_ORDER, CANCEL_WORK_ORDER, CLOSE_WORK_ORDER, DELETE_WORK_ORDER,
                VIEW_PARTS, CREATE_PARTS, USE_PARTS, UPDATE_PARTS, DELETE_PARTS,
                VIEW_TIME_LOGS,
                VIEW_DASHBOARD, VIEW_REPORTS, SEND_NOTIFICATION
        )));

        ROLE_PERMISSIONS.put(MANAGER, new ArrayList<>(List.of(
                LOGIN, LOGOUT,
                CREATE_USER, UPDATE_USER, VIEW_USER,
                CREATE_CUSTOMER, UPDATE_CUSTOMER, VIEW_CUSTOMER,
                CREATE_SITE, UPDATE_SITE, VIEW_SITE,
                CREATE_WORK_ORDER, UPDATE_WORK_ORDER, VIEW_WORK_ORDER,
                ASSIGN_WORK_ORDER, CANCEL_WORK_ORDER, CLOSE_WORK_ORDER,
                VIEW_PARTS, CREATE_PARTS,
                VIEW_TIME_LOGS,
                VIEW_DASHBOARD, VIEW_REPORTS, SEND_NOTIFICATION
        )));

        ROLE_PERMISSIONS.put(DISPATCHER, new ArrayList<>(List.of(
                LOGIN, LOGOUT,
                VIEW_CUSTOMER, VIEW_SITE,
                CREATE_WORK_ORDER, UPDATE_WORK_ORDER, VIEW_WORK_ORDER, ASSIGN_WORK_ORDER,
                VIEW_DASHBOARD
        )));

        ROLE_PERMISSIONS.put(TECHNICIAN, new ArrayList<>(List.of(
                LOGIN, LOGOUT,
                VIEW_WORK_ORDER, VIEW_SITE,
                START_WORK, HOLD_WORK, RESUME_WORK, COMPLETE_WORK,
                VIEW_PARTS, USE_PARTS,
                VIEW_TIME_LOGS, LOG_TIME
        )));

        ROLE_PERMISSIONS.put(CUSTOMER, new ArrayList<>(List.of(
                LOGIN, LOGOUT,
                RAISE_TICKET, VIEW_OWN_TICKETS
        )));
    }

    private RolePermissions() {
        // static holder only - matches the training's point that this must not be
        // instantiated or accessed like a mutable singleton
    }

    public static List<Permission> permissionsFor(Role role) {
        return ROLE_PERMISSIONS.getOrDefault(role, List.of());
    }

    public static boolean hasPermission(Role role, Permission permission) {
        return permissionsFor(role).contains(permission);
    }
}
