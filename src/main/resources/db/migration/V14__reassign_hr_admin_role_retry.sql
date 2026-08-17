-- V13 reverted this account to ADMIN as a stopgap while production was still running
-- the pre-HR_ADMIN jar. The deploy has since completed successfully, so re-apply V12's
-- intent now that the running code actually understands the HR_ADMIN role.
update users set role = 'HR_ADMIN' where email = 'hr@meridiantextiles.com' and role = 'ADMIN';
