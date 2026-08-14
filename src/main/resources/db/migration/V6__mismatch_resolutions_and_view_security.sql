-- Postgres views run with the OWNER's privileges for permission and Row-Level
-- Security purposes by default (security_invoker = false), not the querying role's.
-- v_attendance_mismatches was created by the postgres role, which has BYPASSRLS —
-- so without this, querying the view through the restricted nforcehq_app role would
-- silently bypass tenant isolation and leak every organization's data through it.
alter view v_attendance_mismatches set (security_invoker = true);

-- The view itself is a computed join with no natural row identity to persist
-- "resolved" state against, so a small side table tracks which user/day mismatches
-- a manager has already reviewed and dismissed.
create table attendance_mismatch_resolutions (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    work_date date not null,
    resolved_by uuid not null references users(id),
    resolved_at timestamptz not null default now(),
    unique (organization_id, user_id, work_date)
);

alter table attendance_mismatch_resolutions enable row level security;
create policy tenant_isolation on attendance_mismatch_resolutions
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
