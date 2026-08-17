-- New org-facing roles alongside the existing EMPLOYEE/MANAGER/ADMIN/PLATFORM_ADMIN set.
-- Split into its own migration because Postgres forbids using a newly added enum value
-- in the same transaction that adds it — any data migration referencing these values
-- must live in a later migration file.
alter type user_role add value 'HR_ADMIN';
alter type user_role add value 'SUPER_ADMIN';

-- Profile photo upload — stored as bytea in Postgres, same rationale as leave_attachments
-- (no object storage configured for this deployment).
alter table users
    add column avatar_content_type varchar(100),
    add column avatar_photo bytea;
