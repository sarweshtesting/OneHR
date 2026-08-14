-- Same gap as V5 for attendance_breaks: every JPA entity extends BaseEntity
-- (created_at + updated_at), but this table only got created_at in V1, and a
-- regularization request is genuinely updated in place on approve/reject.

alter table attendance_regularization_requests add column updated_at timestamptz not null default now();
