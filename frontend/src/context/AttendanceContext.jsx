import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { apiFetch } from '../api/client';
import ClockInModal from '../components/ClockInModal';

const AttendanceContext = createContext(null);

/**
 * Shared clock-in state so the Topbar's quick clock control and the Overview page's
 * shift hero stay in sync — each previously called useAttendanceToday() independently,
 * so clocking in from one would leave the other showing stale state until its next fetch.
 */
export function AttendanceProvider({ children }) {
  const [attendance, setAttendance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);

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

  const clockIn = useCallback(async (mode = 'OFFICE', clientId = null) => {
    const data = await apiFetch('/api/attendance/clock-in', { method: 'POST', body: JSON.stringify({ mode, clientId }) });
    setAttendance(data);
  }, []);

  const clockOut = useCallback(async () => {
    const data = await apiFetch('/api/attendance/clock-out', { method: 'POST' });
    setAttendance(data);
  }, []);

  /** Every role picks mode + optional client via a dialog before clocking in. Returns
   * a promise so callers can await/catch it the same way as a direct clockIn() call. */
  const requestClockIn = useCallback(() => {
    setModalOpen(true);
    return Promise.resolve();
  }, []);

  const value = useMemo(() => ({ attendance, loading, reload, clockIn, clockOut, requestClockIn }),
    [attendance, loading, reload, clockIn, clockOut, requestClockIn]);

  return (
    <AttendanceContext.Provider value={value}>
      {children}
      {modalOpen && (
        <ClockInModal
          onClose={() => setModalOpen(false)}
          onConfirm={async (mode, clientId) => {
            await clockIn(mode, clientId);
            setModalOpen(false);
          }}
        />
      )}
    </AttendanceContext.Provider>
  );
}

export function useAttendance() {
  const ctx = useContext(AttendanceContext);
  if (!ctx) throw new Error('useAttendance must be used within AttendanceProvider');
  return ctx;
}
