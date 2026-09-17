package com.zidio.keystone.domain;

/**
 * Every distinct action in the platform, as a fixed set - mirrors the training's approach
 * of defining permissions as an enum rather than free-text strings, so a typo in a permission
 * name is a compile error, not a silent runtime bug.
 */
public enum Permission {
    LOGIN, LOGOUT,

    CREATE_CUSTOMER, UPDATE_CUSTOMER, DELETE_CUSTOMER, VIEW_CUSTOMER,
    CREATE_USER, UPDATE_USER, DELETE_USER, VIEW_USER,
    CREATE_SITE, UPDATE_SITE, VIEW_SITE,

    CREATE_WORK_ORDER, UPDATE_WORK_ORDER, VIEW_WORK_ORDER,
    ASSIGN_WORK_ORDER, CANCEL_WORK_ORDER, CLOSE_WORK_ORDER, DELETE_WORK_ORDER,
    START_WORK, HOLD_WORK, RESUME_WORK, COMPLETE_WORK,

    VIEW_PARTS, CREATE_PARTS, USE_PARTS, UPDATE_PARTS, DELETE_PARTS,
    VIEW_TIME_LOGS, LOG_TIME,

    VIEW_DASHBOARD, VIEW_REPORTS, SEND_NOTIFICATION,

    RAISE_TICKET, VIEW_OWN_TICKETS
}
