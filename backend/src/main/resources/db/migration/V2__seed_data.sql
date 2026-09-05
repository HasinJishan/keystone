-- Seed data. All seed users share the password: Passw0rd!

INSERT INTO customers (id, name, contact_email, contact_phone) VALUES
 (1, 'Meridian Facilities Management', 'ops@meridianfm.example', '+91-9000000001'),
 (2, 'Blue Harbor Logistics', 'facilities@blueharbor.example', '+91-9000000002');

SELECT setval('customers_id_seq', 2);

INSERT INTO sites (id, customer_id, name, address) VALUES
 (1, 1, 'Meridian Tower A', '12 MG Road, Coimbatore'),
 (2, 1, 'Meridian Tower B', '45 Avinashi Road, Coimbatore'),
 (3, 2, 'Blue Harbor Warehouse 1', '9 Port Street, Chennai');

SELECT setval('sites_id_seq', 3);

-- password for all seed users below: Passw0rd!
INSERT INTO users (id, name, email, password_hash, role, customer_id) VALUES
 (1, 'Dana Dispatcher', 'dispatcher@keystone.dev',
    '$2b$10$4HJYSYfgS/iRhrG5mjaof.3wBDkvipES3jxUSarQlqkA1STe2VAg2', 'DISPATCHER', NULL),
 (2, 'Tariq Technician', 'technician@keystone.dev',
    '$2b$10$VJ.4BgwZN8QwIDIaKckBe./IQlYpIE2pnfRAZYR1yBV93Ieaxbez6', 'TECHNICIAN', NULL),
 (3, 'Mona Manager', 'manager@keystone.dev',
    '$2b$10$vOz/0W/awr8yMOwlgBk9r..jqynEutLy7203f4hdQ2uPbVYRjgb36', 'MANAGER', NULL),
 (4, 'Cara Customer', 'customer@keystone.dev',
    '$2b$10$Yam1ID6bdue/Z2dzva8wZupfmnwOsLjY1SE8ic3s6VQEAr1jIwyx2', 'CUSTOMER', 1);

SELECT setval('users_id_seq', 4);

INSERT INTO parts (id, name, sku, unit_cost, stock_qty) VALUES
 (1, 'Air Filter 20x20', 'FLT-2020', 12.50, 40),
 (2, 'Refrigerant R410A (lb)', 'REF-410A', 18.00, 25),
 (3, 'Circuit Breaker 20A', 'ELE-CB20', 9.75, 30),
 (4, 'PVC Pipe Fitting 1in', 'PLB-PVC1', 3.20, 100);

SELECT setval('parts_id_seq', 4);

INSERT INTO work_orders (id, code, title, description, priority, status, customer_id, site_id,
                          assigned_to, created_by, sla_due_at) VALUES
 (1, 'WO-0001', 'HVAC unit not cooling', 'Rooftop AHU-2 blowing warm air.', 'HIGH', 'NEW',
    1, 1, NULL, 1, now() + interval '8 hours'),
 (2, 'WO-0002', 'Breaker tripping in server room', 'Panel B breaker 14 trips repeatedly.', 'CRITICAL', 'ASSIGNED',
    1, 2, 2, 1, now() + interval '4 hours'),
 (3, 'WO-0003', 'Leaking pipe under sink', 'Slow leak, staff placed a bucket.', 'MEDIUM', 'IN_PROGRESS',
    2, 3, 2, 1, now() + interval '24 hours');

SELECT setval('work_orders_id_seq', 3);

INSERT INTO work_order_status_history (work_order_id, from_status, to_status, changed_by, note) VALUES
 (1, NULL, 'NEW', 1, 'Request logged'),
 (2, NULL, 'NEW', 1, 'Request logged'),
 (2, 'NEW', 'ASSIGNED', 1, 'Assigned to Tariq'),
 (3, NULL, 'NEW', 1, 'Request logged'),
 (3, 'NEW', 'ASSIGNED', 1, 'Assigned to Tariq'),
 (3, 'ASSIGNED', 'IN_PROGRESS', 2, 'Started work');
