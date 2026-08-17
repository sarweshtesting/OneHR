-- Removes the two throwaway organizations created while diagnosing the stuck Render
-- deploy above (signup requests used purely to confirm which jar was live).
delete from organizations where slug like 'diag-test%';
