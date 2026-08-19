-- Governance: a simple, append-only trail of admin-ish actions (people added or
-- changed, departments/org edited, leave and regularization decisions).
create table audit_logs (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    actor_user_id uuid references users(id) on delete set null,
    actor_name varchar(200) not null,
    action varchar(50) not null,
    description text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_audit_logs_org_created on audit_logs(organization_id, created_at desc);

alter table audit_logs enable row level security;
create policy tenant_isolation on audit_logs
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
