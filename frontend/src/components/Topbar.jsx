import { useAuth } from '../context/AuthContext';
import { useAttendance } from '../context/AttendanceContext';
import { useClock } from '../hooks/useClock';
import NotificationBell from './NotificationBell';
import UserMenu from './UserMenu';
import GlobalSearch from './GlobalSearch';

export default function Topbar() {
  const { user, organizations, selectedOrgId, selectOrg } = useAuth();
  const { timeString } = useClock();
  const { attendance, loading: attendanceLoading, requestClockIn, clockOut } = useAttendance();

  const isPlatformAdmin = user?.role === 'PLATFORM_ADMIN';
  const currentOrgName = isPlatformAdmin
    ? organizations.find((o) => o.id === selectedOrgId)?.name
    : user?.orgName;

  function handleOrgChange(orgId) {
    selectOrg(orgId);
    // Every page's data hooks fetched under the *previous* org and won't refire on
    // their own — a full reload is the simplest way to guarantee everything currently
    // on screen (stats, approvals, logs, balances…) reflects the newly selected tenant.
    window.location.reload();
  }

  const clockedIn = attendance && attendance.clockInAt && !attendance.clockOutAt;
  // Clocking out is never a dead end — clicking again any time today starts a new
  // session, so the button always reads "Clock in" rather than a disabled "complete" state.
  const clockLabel = clockedIn
    ? (attendance.onBreak ? 'On break' : 'Clock out')
    : 'Clock in';

  async function handleClockToggle() {
    try {
      if (!attendance || !attendance.clockInAt || attendance.clockOutAt) {
        await requestClockIn();
      } else {
        await clockOut();
      }
    } catch (err) {
      alert(err.message);
    }
  }

  return (
    <header className="topbar">
      <div className="org-switch">
        <div className="avatar-sq" />
        {isPlatformAdmin ? (
          <select className="org-select" value={selectedOrgId || ''} onChange={(e) => handleOrgChange(e.target.value)}>
            {organizations.map((o) => (
              <option key={o.id} value={o.id}>{o.name}</option>
            ))}
          </select>
        ) : (
          <span>{currentOrgName || '—'}</span>
        )}
      </div>

      <GlobalSearch />

      <div className="topbar-right">
        <div className="mini-clock"><span className="dot" /><span>{timeString} IST</span></div>
        <button
          className={'topbar-clock-btn' + (clockedIn ? (attendance?.onBreak ? ' on-break' : ' on') : '')}
          onClick={handleClockToggle}
          disabled={attendanceLoading}
          title={clockLabel}
        >
          <span className="topbar-clock-dot" />
          <span>{clockLabel}</span>
        </button>
        <NotificationBell />
        <UserMenu orgName={currentOrgName} />
      </div>
    </header>
  );
}
