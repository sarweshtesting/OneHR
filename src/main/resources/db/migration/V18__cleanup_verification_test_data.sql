-- Removes the throwaway account and regularization request created while manually
-- verifying the new POST /api/people and regularization endpoints.
delete from users where email = 'test.newhire@meridiantextiles.com';
delete from attendance_regularization_requests where reason = 'Forgot to clock in test';
