-- Lets an employee clock back in after already checking out today (e.g. went out
-- and came back, or checked out by mistake) instead of being locked out for the day.
-- banked_minutes carries forward the worked time from prior sessions so a later
-- clock-out can add this session's time on top rather than overwriting it.
alter table attendance_records add column banked_minutes integer not null default 0;
