-- Same class of bug as V3, in the two policies V3 didn't touch: even though
-- current_setting('app.tenant_id', true) = '' short-circuits the boolean OR in the
-- application's intended reading, Postgres does not guarantee that a later branch's
-- ::uuid cast is skipped at execution time, and it throws instead of evaluating to
-- false. Guard every remaining bare cast the same way — nullif(..., '')::uuid, which
-- evaluates to NULL (a safe non-match) instead of raising an error.

drop policy tenant_isolation on organizations;
create policy tenant_isolation on organizations
    using (
        current_setting('app.tenant_id', true) is null
        or current_setting('app.tenant_id', true) = ''
        or id = nullif(current_setting('app.tenant_id', true), '')::uuid
    );

drop policy tenant_isolation on users;
create policy tenant_isolation on users
    using (
        current_setting('app.tenant_id', true) is null
        or current_setting('app.tenant_id', true) = ''
        or organization_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        or id = nullif(current_setting('app.user_id', true), '')::uuid
    );
