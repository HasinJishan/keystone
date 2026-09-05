-- Project KEYSTONE - initial schema

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    email           VARCHAR(200) NOT NULL UNIQUE,
    password_hash   VARCHAR(200) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('DISPATCHER','TECHNICIAN','MANAGER','CUSTOMER')),
    customer_id     BIGINT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE customers (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    contact_email   VARCHAR(200),
    contact_phone   VARCHAR(50),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE users ADD CONSTRAINT fk_users_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);

CREATE TABLE sites (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers(id),
    name            VARCHAR(200) NOT NULL,
    address         VARCHAR(300),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_sites_customer ON sites(customer_id);

CREATE TABLE parts (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    sku             VARCHAR(50) NOT NULL UNIQUE,
    unit_cost       NUMERIC(10,2) NOT NULL DEFAULT 0,
    stock_qty       INTEGER NOT NULL DEFAULT 0 CHECK (stock_qty >= 0)
);

CREATE TABLE work_orders (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(30) NOT NULL UNIQUE,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    priority        VARCHAR(10) NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    status          VARCHAR(20) NOT NULL CHECK (status IN
                        ('NEW','ASSIGNED','IN_PROGRESS','ON_HOLD','COMPLETED','CLOSED','CANCELLED')),
    customer_id     BIGINT NOT NULL REFERENCES customers(id),
    site_id         BIGINT NOT NULL REFERENCES sites(id),
    assigned_to     BIGINT NULL REFERENCES users(id),
    created_by      BIGINT NULL REFERENCES users(id),
    sla_due_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    completed_at    TIMESTAMP NULL,
    closed_at       TIMESTAMP NULL,
    sla_breached    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_wo_status ON work_orders(status);
CREATE INDEX idx_wo_customer ON work_orders(customer_id);
CREATE INDEX idx_wo_assigned ON work_orders(assigned_to);
CREATE INDEX idx_wo_sla_due ON work_orders(sla_due_at);

CREATE TABLE work_order_status_history (
    id              BIGSERIAL PRIMARY KEY,
    work_order_id   BIGINT NOT NULL REFERENCES work_orders(id),
    from_status     VARCHAR(20),
    to_status       VARCHAR(20) NOT NULL,
    changed_by      BIGINT REFERENCES users(id),
    changed_at      TIMESTAMP NOT NULL DEFAULT now(),
    note            VARCHAR(500)
);

CREATE INDEX idx_history_wo ON work_order_status_history(work_order_id);

CREATE TABLE part_usage (
    id              BIGSERIAL PRIMARY KEY,
    work_order_id   BIGINT NOT NULL REFERENCES work_orders(id),
    part_id         BIGINT NOT NULL REFERENCES parts(id),
    qty_used        INTEGER NOT NULL CHECK (qty_used > 0),
    logged_by       BIGINT REFERENCES users(id),
    logged_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_partusage_wo ON part_usage(work_order_id);

CREATE TABLE time_logs (
    id              BIGSERIAL PRIMARY KEY,
    work_order_id   BIGINT NOT NULL REFERENCES work_orders(id),
    technician_id   BIGINT NOT NULL REFERENCES users(id),
    minutes         INTEGER NOT NULL CHECK (minutes > 0),
    note            VARCHAR(500),
    logged_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_timelog_wo ON time_logs(work_order_id);
