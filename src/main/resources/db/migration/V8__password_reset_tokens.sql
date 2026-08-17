-- Supports the forgot/reset-password flow. No organization_id / RLS here on
-- purpose: this table is looked up by token hash before any tenant context can be
-- known (the requester isn't authenticated yet), the same reason users/organizations
-- carry a pre-auth carve-out in their own policies. It holds nothing sensitive beyond
-- a hashed, single-use, short-lived token, so a missing policy isn't a tenant leak.
create table password_reset_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    token_hash varchar(255) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);

create index idx_password_reset_tokens_user on password_reset_tokens(user_id);
