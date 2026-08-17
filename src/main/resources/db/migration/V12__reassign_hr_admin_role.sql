-- The seeded "Meridian HR" demo account is titled HR Admin already; give it the
-- matching role now that HR_ADMIN exists. Guarded so it's a no-op if already changed.
update users set role = 'HR_ADMIN' where email = 'hr@meridiantextiles.com' and role = 'ADMIN';
