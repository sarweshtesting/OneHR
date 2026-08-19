-- Lets clock-in optionally record which client the employee is working for that
-- day; clock-out then auto-logs the worked hours against that client.
alter table attendance_records add column client_id uuid references clients(id);
