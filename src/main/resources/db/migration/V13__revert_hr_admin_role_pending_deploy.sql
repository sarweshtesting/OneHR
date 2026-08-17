-- Temporary mitigation: production's backend deploy of V11/V12 (which added the
-- HR_ADMIN/SUPER_ADMIN enum values) is stuck behind the old jar, which doesn't know
-- the HR_ADMIN label and 500s on any query touching this row. Revert until the actual
-- deploy completes; V12's UPDATE can be re-applied by hand once it has.
update users set role = 'ADMIN' where email = 'hr@meridiantextiles.com' and role = 'HR_ADMIN';
