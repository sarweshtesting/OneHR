-- Personal/employment/emergency-contact fields for the profile page.
alter table users
    add column blood_group varchar(10),
    add column phone varchar(30),
    add column date_of_birth date,
    add column hire_date date,
    add column emergency_contact_name varchar(200),
    add column emergency_contact_relationship varchar(100),
    add column emergency_contact_phone varchar(30);

-- Notifications: generated when a leave/regularization request is submitted (notifies
-- the approver) or decided (notifies the requester). type is a plain varchar rather
-- than a native enum — see the earlier Hibernate/native-enum binding issues — kept
-- simple since this table has no other consumer that needs it strongly typed in SQL.
create table notifications (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    type varchar(50) not null,
    title varchar(255) not null,
    body text,
    related_id uuid,
    is_read boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_notifications_user_created on notifications(user_id, created_at desc);

alter table notifications enable row level security;
create policy tenant_isolation on notifications
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- Per-organization holiday calendar, shown on the Calendar tab.
create table holidays (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    holiday_date date not null,
    name varchar(255) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (organization_id, holiday_date)
);

alter table holidays enable row level security;
create policy tenant_isolation on holidays
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

insert into holidays (organization_id, holiday_date, name) values
    ('a0000000-0000-0000-0000-000000000001', date '2026-08-15', 'Independence Day'),
    ('a0000000-0000-0000-0000-000000000001', date '2026-10-02', 'Gandhi Jayanti'),
    ('a0000000-0000-0000-0000-000000000001', date '2026-12-25', 'Christmas Day'),
    ('a0000000-0000-0000-0000-000000000002', date '2026-08-15', 'Independence Day'),
    ('a0000000-0000-0000-0000-000000000002', date '2026-10-02', 'Gandhi Jayanti'),
    ('a0000000-0000-0000-0000-000000000002', date '2026-12-25', 'Christmas Day');
