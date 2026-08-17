-- Simplify the org-facing role set to EMPLOYEE / MANAGER / HR_ADMIN / SUPER_ADMIN.
-- PLATFORM_ADMIN is untouched — it's nforceone.com's cross-tenant staff role, not one
-- of the roles an organization assigns to its own people.
--
-- ADMIN has had zero real accounts since HR_ADMIN was introduced (V11-V14); the only
-- rows still on it are throwaway orgs created by browser-based signup testing in prior
-- sessions. Remove those orgs first so the enum can be recreated without ADMIN at all.
delete from organizations where slug in ('acme-textiles', 'browser-test-co', 'deploy-check', 'live-test-co');

-- The CHECK constraint's 'PLATFORM_ADMIN' literal is bound to the current enum type at
-- parse time, so it must be dropped before the type swap and recreated after — otherwise
-- Postgres tries to compare the new-typed column against an old-typed literal.
alter table users drop constraint chk_platform_admin_org;

alter type user_role rename to user_role_old;
create type user_role as enum ('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN');
alter table users alter column role type user_role using role::text::user_role;
drop type user_role_old;

alter table users add constraint chk_platform_admin_org check (
    (role = 'PLATFORM_ADMIN' and organization_id is null) or
    (role <> 'PLATFORM_ADMIN' and organization_id is not null)
);
