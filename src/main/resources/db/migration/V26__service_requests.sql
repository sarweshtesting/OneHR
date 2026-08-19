-- HR Service Requests: a lightweight ticketing channel so employees can raise
-- HR/IT/payroll queries without an email thread, and HR gets a single inbox.
create type service_request_type as enum ('HR_QUERY', 'DOCUMENT_REQUEST', 'IT_SUPPORT', 'PAYROLL_QUERY', 'OTHER');
create type service_request_status as enum ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');

create table service_requests (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    requester_id uuid not null references users(id),
    type service_request_type not null,
    subject varchar(200) not null,
    description text not null,
    status service_request_status not null default 'OPEN',
    assignee_id uuid references users(id),
    resolution_notes text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_service_requests_org on service_requests(organization_id);
create index idx_service_requests_requester on service_requests(organization_id, requester_id);

alter table service_requests enable row level security;
create policy tenant_isolation on service_requests
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
