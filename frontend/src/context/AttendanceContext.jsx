import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { apiFetch } from '../api/client';

const AttendanceContext = createContext(null);

/**
 * Shared clock-in state so the Topbar's quick clock control and the Overview page's
 * shift hero stay in sync — each previously called useAttendanceToday() independently,
 * so clocking in from one would leave the other showing stale state until its next fetch.
 */
export function AttendanceProvider({ children }) {
  const [attendance, setAttendance] = useState(null);
  const [loading, setLoading] = useState(true);

  const reload = useCallback(async () => {
    try {
      const data = await apiFetch('/api/attendance/me/today');
      setAttendance(data);
    } catch (err) {
      console.error('Failed to load attendance', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { reload(); }, [reload]);

  const clockIn = useCallback(async (mode = 'OFFICE') => {
    const data = await apiFetch('/api/attendance/clock-in', { method: 'POST', body: JSON.stringify({ mode }) });
    setAttendance(data);
  }, []);

  const clockOut = useCallback(async () => {
    const data = await apiFetch('/api/attendance/clock-out', { method: 'POST' });
    setAttendance(data);
  }, []);

  const value = useMemo(() => ({ attendance, loading, reload, clockIn, clockOut }), [attendance, loading, reload, clockIn, clockOut]);

  return <AttendanceContext.Provider value={value}>{children}</AttendanceContext.Provider>;
}

export function useAttendance() {
  const ctx = useContext(AttendanceContext);
  if (!ctx) throw new Error('useAttendance must be used within AttendanceProvider');
  return ctx;
}
