-- V1's "strict" tenant policies cast current_setting('app.tenant_id', true) straight
-- to uuid. That's fine when the setting is genuinely unset (NULL) or a real UUID, but
-- the application always writes a concrete value on every connection checkout,
-- including an empty string when no tenant is selected yet (e.g. a PLATFORM_ADMIN who
-- hasn't picked an org) — and ''::uuid raises an error instead of denying the row.
-- Using nullif(..., '') makes an empty string coerce to NULL first, so the comparison
-- evaluates to "no match" (deny) instead of throwing.

drop policy tenant_isolation on departments;
create policy tenant_isolation on departments
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

drop policy tenant_isolation on leave_types;
create policy tenant_isolation on leave_types
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

drop policy tenant_isolation on leave_requests;
create policy tenant_isolation on leave_requests
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

drop policy tenant_isolation on attendance_records;
create policy tenant_isolation on attendance_records
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

drop policy tenant_isolation on attendance_regularization_requests;
create policy tenant_isolation on attendance_regularization_requests
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

drop policy tenant_isolation on client_logs;
create policy tenant_isolation on client_logs
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

drop policy tenant_isolation on announcements;
create policy tenant_isolation on announcements
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

drop policy tenant_isolation on todos;
create policy tenant_isolation on todos
    using (organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
