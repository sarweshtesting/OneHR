-- Removes the throwaway client-log row created while manually verifying the new
-- POST /api/client-logs endpoint.
delete from client_logs where client_name = 'Test Client Co';
