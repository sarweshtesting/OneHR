import { useCallback, useEffect, useState } from 'react';
import { apiFetch } from '../api/client';

export function useAttendanceToday() {
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

  return { attendance, loading, reload, clockIn, clockOut };
}
