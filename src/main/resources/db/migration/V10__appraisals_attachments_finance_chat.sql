-- Collapsible appraisal records shown on the Appraisal tab.
create table appraisal_reviews (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    reviewer_id uuid references users(id),
    cycle_name varchar(100) not null,
    overall_rating varchar(50),
    strengths text,
    areas_for_improvement text,
    goals_next_cycle text,
    status varchar(20) not null default 'DRAFT',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_appraisal_reviews_user on appraisal_reviews(user_id, created_at desc);

alter table appraisal_reviews enable row level security;
create policy tenant_isolation on appraisal_reviews
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- Attachments (certificates, medical notes, etc.) uploaded against a leave request.
-- Stored as bytea in Postgres rather than external object storage — no S3/blob
-- store is configured for this deployment, and these are small, infrequent files.
create table leave_attachments (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    leave_request_id uuid not null references leave_requests(id) on delete cascade,
    uploaded_by uuid not null references users(id),
    file_name varchar(255) not null,
    content_type varchar(100) not null,
    file_size int not null,
    file_data bytea not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_leave_attachments_request on leave_attachments(leave_request_id);

alter table leave_attachments enable row level security;
create policy tenant_isolation on leave_attachments
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- Single org-wide channel for the Finance tab's chat feature (polling-based, matching
-- this app's plain-REST style — no WebSocket infrastructure exists elsewhere yet).
create table finance_chat_messages (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id),
    message text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_finance_chat_org_created on finance_chat_messages(organization_id, created_at asc);

alter table finance_chat_messages enable row level security;
create policy tenant_isolation on finance_chat_messages
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
