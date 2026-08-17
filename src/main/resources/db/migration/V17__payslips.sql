-- Finance & Ops, first slice: payslips. Every employee can see their own; managers and
-- above can see the whole organization's. No PDF generation yet — structured data only.
create table payslips (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    period_month date not null,
    gross_pay numeric(12,2) not null,
    deductions numeric(12,2) not null default 0,
    net_pay numeric(12,2) not null,
    status varchar(20) not null default 'PAID',
    generated_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, period_month)
);
create index idx_payslips_org on payslips(organization_id);
create index idx_payslips_user_period on payslips(user_id, period_month desc);

alter table payslips enable row level security;
create policy tenant_isolation on payslips
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- Demo payslip history (last 3 months) for the seeded Meridian Textiles and Aurora
-- Logistics staff, so the new Payslips view isn't empty on first look.
insert into payslips (organization_id, user_id, period_month, gross_pay, deductions, net_pay, status)
select
    u.organization_id,
    u.id,
    date_trunc('month', now())::date - (interval '1 month' * gs.months_ago),
    base.gross,
    round(base.gross * 0.18, 2),
    round(base.gross * 0.82, 2),
    case when gs.months_ago = 0 then 'GENERATED' else 'PAID' end
from users u
cross join generate_series(0, 2) as gs(months_ago)
cross join lateral (
    select case u.role
        when 'MANAGER' then 145000.00
        when 'HR_ADMIN' then 110000.00
        else 90000.00
    end as gross
) base
where u.organization_id is not null and u.is_active;
