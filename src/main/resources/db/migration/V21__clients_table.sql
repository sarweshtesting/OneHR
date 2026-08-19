-- A real client roster, so logging hours can select a known client instead of
-- retyping a freeform name every time — while still allowing a brand-new client's
-- details to be entered inline as part of the same submission.
create table clients (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    name varchar(150) not null,
    contact_person varchar(150),
    notes text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (organization_id, name)
);
create index idx_clients_org on clients(organization_id);

alter table clients enable row level security;
create policy tenant_isolation on clients
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

alter table client_logs add column client_id uuid references clients(id);

-- Backfill: turn every distinct (organization, client_name) already logged into a
-- real client row, then point the existing log rows at it.
insert into clients (organization_id, name)
select distinct organization_id, client_name
from client_logs
where client_name is not null
on conflict (organization_id, name) do nothing;

update client_logs cl
set client_id = c.id
from clients c
where c.organization_id = cl.organization_id and c.name = cl.client_name and cl.client_id is null;
