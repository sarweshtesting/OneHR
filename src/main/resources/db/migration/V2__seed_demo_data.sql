-- Demo data: two organizations (the second exists purely to prove tenant isolation),
-- matching the people already shown in the nForceHQ-app.html mock, plus one
-- PLATFORM_ADMIN account for nforceone.com staff.
--
-- All seeded passwords are the literal string 'Passw0rd!' — bcrypt-hashed here via
-- pgcrypto's crypt()/gen_salt('bf'), which produces standard $2a$ bcrypt hashes
-- fully compatible with Spring Security's BCryptPasswordEncoder. Change on first login.

insert into organizations (id, name, slug, timezone) values
    ('a0000000-0000-0000-0000-000000000001', 'Meridian Textiles', 'meridian-textiles', 'Asia/Kolkata'),
    ('a0000000-0000-0000-0000-000000000002', 'Aurora Logistics', 'aurora-logistics', 'Asia/Kolkata');

insert into departments (id, organization_id, name) values
    ('d0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Engineering'),
    ('d0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Design'),
    ('d0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'QA'),
    ('d0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'DevOps'),
    ('d0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000002', 'Operations');

-- Platform admin (nforceone.com) — no organization_id, spans all client orgs via
-- the X-Organization-Id header once authenticated.
insert into users (id, organization_id, employee_code, full_name, email, password_hash, role, avatar_initials, is_active) values
    ('b0000000-0000-0000-0000-000000000000', null, 'NF-001', 'Santosh Das', 'santosh.das@nforceone.com', crypt('Passw0rd!', gen_salt('bf')), 'PLATFORM_ADMIN', 'SD', true);

-- Meridian Textiles staff (matches the HTML mock)
insert into users (id, organization_id, employee_code, full_name, email, password_hash, role, department_id, job_title, avatar_initials, is_active) values
    ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'MT-001', 'Priya Nair',    'priya.nair@meridiantextiles.com',   crypt('Passw0rd!', gen_salt('bf')), 'MANAGER',  'd0000000-0000-0000-0000-000000000001', 'Engineering Manager', 'PN', true),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'MT-002', 'Arjun Kapoor',  'arjun.kapoor@meridiantextiles.com', crypt('Passw0rd!', gen_salt('bf')), 'EMPLOYEE', 'd0000000-0000-0000-0000-000000000001', 'Backend Engineer',    'AK', true),
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'MT-003', 'Sara Mathew',   'sara.mathew@meridiantextiles.com',  crypt('Passw0rd!', gen_salt('bf')), 'EMPLOYEE', 'd0000000-0000-0000-0000-000000000002', 'Product Designer',    'SM', true),
    ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'MT-004', 'Rohit Desai',   'rohit.desai@meridiantextiles.com',  crypt('Passw0rd!', gen_salt('bf')), 'EMPLOYEE', 'd0000000-0000-0000-0000-000000000003', 'QA Engineer',         'RD', true),
    ('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'MT-005', 'Lena Fischer',  'lena.fischer@meridiantextiles.com', crypt('Passw0rd!', gen_salt('bf')), 'EMPLOYEE', 'd0000000-0000-0000-0000-000000000001', 'Backend Engineer',    'LF', true),
    ('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'MT-006', 'Tariq Wasi',    'tariq.wasi@meridiantextiles.com',   crypt('Passw0rd!', gen_salt('bf')), 'EMPLOYEE', 'd0000000-0000-0000-0000-000000000004', 'DevOps Engineer',     'TW', true),
    ('b0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000001', 'MT-007', 'Meridian HR',   'hr@meridiantextiles.com',           crypt('Passw0rd!', gen_salt('bf')), 'ADMIN',    null,                                     'HR Admin',            'MH', true);

update users set manager_id = 'b0000000-0000-0000-0000-000000000001'
where id in (
    'b0000000-0000-0000-0000-000000000002',
    'b0000000-0000-0000-0000-000000000003',
    'b0000000-0000-0000-0000-000000000004',
    'b0000000-0000-0000-0000-000000000005',
    'b0000000-0000-0000-0000-000000000006'
);

-- Aurora Logistics — a second tenant with no relation to Meridian, used to verify
-- that RLS + application filtering actually prevent cross-tenant reads.
insert into users (id, organization_id, employee_code, full_name, email, password_hash, role, department_id, job_title, avatar_initials, is_active) values
    ('b0000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000002', 'AL-001', 'Dana Cole', 'dana.cole@auroralogistics.com', crypt('Passw0rd!', gen_salt('bf')), 'MANAGER', 'd0000000-0000-0000-0000-000000000005', 'Operations Manager', 'DC', true);

-- Leave types + balances (current year) for Meridian Textiles
insert into leave_types (id, organization_id, code, name, color_token, annual_quota_days) values
    ('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'EARNED',   'Earned leave', 'brand',  20),
    ('e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'SICK',     'Sick leave',   'moss',   10),
    ('e0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'CASUAL',   'Casual leave', 'amber',  6),
    ('e0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'COMP_OFF', 'Comp-off',     'violet', 4),
    ('e0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'UNPAID',   'Unpaid leave', 'ink',    0);

insert into leave_balances (user_id, leave_type_id, year, allocated_days, used_days) values
    ('b0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', extract(year from now())::int, 20, 6),
    ('b0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', extract(year from now())::int, 10, 4),
    ('b0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', extract(year from now())::int, 6,  4),
    ('b0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000004', extract(year from now())::int, 4,  3);
