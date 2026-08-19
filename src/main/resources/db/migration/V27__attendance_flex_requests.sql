-- WFH / partial-day / overtime requests: distinct from regularization (which
-- corrects a day's recorded clock times) — these are forward/after-the-fact
-- asks for approval that don't touch attendance_records directly.
create type flex_request_type as enum ('WFH', 'PARTIAL_DAY_LATE_ARRIVAL', 'PARTIAL_DAY_LEAVING_EARLY', 'OVERTIME');

create table attendance_flex_requests (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id),
    type flex_request_type not null,
    work_date date not null,
    hours numeric(4,1),
    reason text,
    status regularization_status not null default 'PENDING',
    approver_id uuid references users(id),
    decided_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_flex_requests_org_user on attendance_flex_requests(organization_id, user_id);
create index idx_flex_requests_org_status on attendance_flex_requests(organization_id, status);

alter table attendance_flex_requests enable row level security;
create policy tenant_isolation on attendance_flex_requests
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
