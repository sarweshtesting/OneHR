-- nForceHQ initial schema: organizations, people, attendance, leave.
-- Multi-tenant via a shared schema + organization_id discriminator column,
-- enforced by Row-Level Security policies below in addition to application-level filtering.

create extension if not exists pgcrypto;

create type user_role as enum ('EMPLOYEE','MANAGER','ADMIN','PLATFORM_ADMIN');
create type leave_type_code as enum ('EARNED','SICK','CASUAL','COMP_OFF','UNPAID');
create type leave_request_status as enum ('PENDING','APPROVED','REJECTED','CANCELLED');
create type attendance_mode as enum ('OFFICE','WFH');
create type attendance_status as enum ('IN_PROGRESS','ON_TIME','LATE','ABSENT','ON_LEAVE');
create type attendance_source as enum ('WEB_CLOCK','MANUAL','REGULARIZED');
create type break_type as enum ('LUNCH','SHORT','OTHER');
create type regularization_status as enum ('PENDING','APPROVED','REJECTED');

-- ============================================================
-- Organizations / people
-- ============================================================

create table organizations (
    id uuid primary key default gen_random_uuid(),
    name varchar(200) not null,
    slug varchar(100) not null unique,
    timezone varchar(50) not null default 'Asia/Kolkata',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table departments (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    name varchar(150) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (organization_id, name)
);

-- organization_id is nullable only for PLATFORM_ADMIN users (nforceone.com staff who
-- operate across multiple client organizations rather than belonging to one).
-- email is globally unique (not per-organization) so login-by-email can resolve a
-- user before their organization/tenant is known.
create table users (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid references organizations(id) on delete cascade,
    employee_code varchar(50),
    full_name varchar(200) not null,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    role user_role not null,
    department_id uuid references departments(id) on delete set null,
    manager_id uuid references users(id) on delete set null,
    job_title varchar(150),
    avatar_initials varchar(4),
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_platform_admin_org check (
        (role = 'PLATFORM_ADMIN' and organization_id is null) or
        (role <> 'PLATFORM_ADMIN' and organization_id is not null)
    )
);
create index idx_users_organization_id on users(organization_id);
create index idx_users_manager_id on users(manager_id);
create index idx_users_department_id on users(department_id);

-- ============================================================
-- Leave
-- ============================================================

create table leave_types (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    code leave_type_code not null,
    name varchar(100) not null,
    color_token varchar(30),
    annual_quota_days numeric(5,2) not null default 0,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (organization_id, code)
);

create table leave_balances (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    leave_type_id uuid not null references leave_types(id) on delete cascade,
    year int not null,
    allocated_days numeric(5,2) not null default 0,
    used_days numeric(5,2) not null default 0,
    updated_at timestamptz not null default now(),
    unique (user_id, leave_type_id, year)
);

create table leave_requests (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    leave_type_id uuid not null references leave_types(id),
    start_date date not null,
    end_date date not null,
    days_requested numeric(5,2) not null,
    reason text,
    status leave_request_status not null default 'PENDING',
    approver_id uuid references users(id),
    decided_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_leave_requests_org_status on leave_requests(organization_id, status);
create index idx_leave_requests_user on leave_requests(user_id);

-- ============================================================
-- Attendance
-- ============================================================

create table attendance_records (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    work_date date not null,
    clock_in_at timestamptz,
    clock_out_at timestamptz,
    mode attendance_mode,
    status attendance_status not null default 'IN_PROGRESS',
    total_break_minutes int not null default 0,
    total_worked_minutes int,
    source attendance_source not null default 'WEB_CLOCK',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, work_date)
);
create index idx_attendance_org_date on attendance_records(organization_id, work_date);

create table attendance_breaks (
    id uuid primary key default gen_random_uuid(),
    attendance_record_id uuid not null references attendance_records(id) on delete cascade,
    break_start_at timestamptz not null,
    break_end_at timestamptz,
    break_type break_type not null default 'OTHER',
    created_at timestamptz not null default now()
);
create index idx_attendance_breaks_record on attendance_breaks(attendance_record_id);

create table attendance_regularization_requests (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    work_date date not null,
    requested_clock_in timestamptz,
    requested_clock_out timestamptz,
    reason text,
    status regularization_status not null default 'PENDING',
    approver_id uuid references users(id),
    decided_at timestamptz,
    resulting_attendance_record_id uuid references attendance_records(id),
    created_at timestamptz not null default now()
);
create index idx_regularization_org_status on attendance_regularization_requests(organization_id, status);

create table client_logs (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    work_date date not null,
    client_name varchar(150),
    logged_hours numeric(5,2) not null,
    source varchar(50),
    created_at timestamptz not null default now()
);
create index idx_client_logs_org_user_date on client_logs(organization_id, user_id, work_date);

-- Joins attendance vs client-reported hours for the same user/day; backs both the
-- "mismatches flagged" dashboard stat and the approvals-inbox mismatch cards.
create view v_attendance_mismatches as
select
    a.organization_id,
    a.user_id,
    a.work_date,
    a.total_worked_minutes,
    round(a.total_worked_minutes / 60.0, 2) as internal_hours,
    c.client_name,
    c.logged_hours as client_hours,
    abs(coalesce(c.logged_hours, 0) - round(a.total_worked_minutes / 60.0, 2)) as hours_delta
from attendance_records a
join client_logs c
    on c.organization_id = a.organization_id
    and c.user_id = a.user_id
    and c.work_date = a.work_date
where a.total_worked_minutes is not null
  and abs(coalesce(c.logged_hours, 0) - round(a.total_worked_minutes / 60.0, 2)) > 1.0;

-- ============================================================
-- Overview widgets
-- ============================================================

create table announcements (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    tag varchar(50),
    title varchar(255) not null,
    body text,
    author_id uuid references users(id),
    published_at timestamptz not null default now(),
    expires_at timestamptz
);
create index idx_announcements_org_published on announcements(organization_id, published_at desc);

create table todos (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    title varchar(255) not null,
    tag varchar(50),
    is_done boolean not null default false,
    due_at timestamptz,
    created_at timestamptz not null default now()
);
create index idx_todos_org_user on todos(organization_id, user_id);

-- ============================================================
-- Row Level Security
-- ============================================================
-- The application sets the Postgres session variable app.tenant_id on every
-- connection checkout (see TenantAwareDataSource), based on the caller's JWT
-- organization claim, or an explicit X-Organization-Id header for PLATFORM_ADMIN.
--
-- An empty/unset app.tenant_id means "no tenant selected yet" and is only ever
-- produced by two legitimate code paths: the pre-authentication login lookup
-- (by globally-unique email, before we know which org the user belongs to) and
-- a PLATFORM_ADMIN who hasn't picked a target organization yet. Every other
-- table has no such carve-out, since nothing else legitimately queries them
-- before a tenant is resolved.

alter table organizations enable row level security;
create policy tenant_isolation on organizations
    using (
        current_setting('app.tenant_id', true) is null
        or current_setting('app.tenant_id', true) = ''
        or id = current_setting('app.tenant_id', true)::uuid
    );

-- A user can always see their own row (even a PLATFORM_ADMIN with no organization_id,
-- while some other org is selected via app.tenant_id) in addition to the normal
-- tenant-scoped and pre-auth carve-outs.
alter table users enable row level security;
create policy tenant_isolation on users
    using (
        current_setting('app.tenant_id', true) is null
        or current_setting('app.tenant_id', true) = ''
        or organization_id = current_setting('app.tenant_id', true)::uuid
        or id = nullif(current_setting('app.user_id', true), '')::uuid
    );

alter table departments enable row level security;
create policy tenant_isolation on departments
    using (organization_id = current_setting('app.tenant_id', true)::uuid);

alter table leave_types enable row level security;
create policy tenant_isolation on leave_types
    using (organization_id = current_setting('app.tenant_id', true)::uuid);

alter table leave_requests enable row level security;
create policy tenant_isolation on leave_requests
    using (organization_id = current_setting('app.tenant_id', true)::uuid);

alter table attendance_records enable row level security;
create policy tenant_isolation on attendance_records
    using (organization_id = current_setting('app.tenant_id', true)::uuid);

alter table attendance_regularization_requests enable row level security;
create policy tenant_isolation on attendance_regularization_requests
    using (organization_id = current_setting('app.tenant_id', true)::uuid);

alter table client_logs enable row level security;
create policy tenant_isolation on client_logs
    using (organization_id = current_setting('app.tenant_id', true)::uuid);

alter table announcements enable row level security;
create policy tenant_isolation on announcements
    using (organization_id = current_setting('app.tenant_id', true)::uuid);

alter table todos enable row level security;
create policy tenant_isolation on todos
    using (organization_id = current_setting('app.tenant_id', true)::uuid);

-- leave_balances and attendance_breaks have no organization_id of their own; they are
-- scoped indirectly through their parent (user_id / attendance_record_id) via explicit
-- joins/filters in the application/service layer.
