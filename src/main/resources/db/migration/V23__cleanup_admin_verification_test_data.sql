-- Removes throwaway data created while manually verifying the new Admin/Client
-- Tracking endpoints (department, client, and log entries created purely for testing).
delete from client_logs where client_id in (select id from clients where name = 'Test Verify Co');
delete from client_logs where client_name = 'Acme Client Corp' and work_date = date '2026-08-19' and logged_hours = 3;
delete from clients where name = 'Test Verify Co';
delete from departments where name = 'Customer Success';
