-- client_logs only had created_at in V1 (same gap V5/V7 already fixed elsewhere) —
-- bring it in line so it can extend the standard BaseEntity like every other table.
alter table client_logs add column updated_at timestamptz not null default now();

-- Demo Super Admin account for Meridian Textiles/Nexora, so all four org-facing
-- roles (Employee, Manager, HR Admin, Super Admin) have a real seeded login —
-- previously the only way to get a SUPER_ADMIN account was self-signup, which
-- creates a whole new organization rather than fitting into the existing demo org.
insert into users (id, organization_id, employee_code, full_name, email, password_hash, role, job_title, avatar_initials, is_active) values
    ('b0000000-0000-0000-0000-000000000020', 'a0000000-0000-0000-0000-000000000001', 'MT-020', 'Nexora Super Admin', 'superadmin@meridiantextiles.com', crypt('Passw0rd!', gen_salt('bf')), 'SUPER_ADMIN', 'Founder', 'NS', true);

-- A little demo client-hours history so the new Client Tracking page isn't empty.
insert into client_logs (organization_id, user_id, work_date, client_name, logged_hours, source)
select
    'a0000000-0000-0000-0000-000000000001'::uuid,
    u.id,
    d::date,
    'Aurora Retail Co',
    6.5,
    'MANUAL'
from (values
    ('b0000000-0000-0000-0000-000000000002'::uuid),
    ('b0000000-0000-0000-0000-000000000003'::uuid),
    ('b0000000-0000-0000-0000-000000000005'::uuid)
) as u(id)
cross join generate_series(current_date - interval '2 days', current_date, interval '1 day') as d;
