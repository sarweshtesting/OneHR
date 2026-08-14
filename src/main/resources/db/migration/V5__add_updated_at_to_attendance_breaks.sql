-- attendance_breaks only had created_at in V1, but every JPA entity extends
-- BaseEntity (created_at + updated_at) for consistency, and breaks are genuinely
-- updated in place when a break is closed (break_end_at set). Bring the table in
-- line with that instead of giving this one entity a special-cased base class.

alter table attendance_breaks add column updated_at timestamptz not null default now();
