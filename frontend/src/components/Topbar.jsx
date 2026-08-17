import { useAuth } from '../context/AuthContext';
import { useAttendance } from '../context/AttendanceContext';
import { useClock } from '../hooks/useClock';
import { IconSearch } from './icons';
import NotificationBell from './NotificationBell';
import UserMenu from './UserMenu';

export default function Topbar() {
  const { user, organizations, selectedOrgId, selectOrg } = useAuth();
  const { timeString } = useClock();
  const { attendance, clockIn, clockOut } = useAttendance();

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
  const shiftComplete = attendance && attendance.clockInAt && attendance.clockOutAt;
  const clockLabel = !attendance || !attendance.clockInAt
    ? 'Clock in'
    : !attendance.clockOutAt
      ? (attendance.onBreak ? 'On break' : 'Clock out')
      : 'Shift complete';

  async function handleClockToggle() {
    try {
      if (!attendance || !attendance.clockInAt || attendance.clockOutAt) {
        await clockIn('OFFICE');
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

      <div className="search-box">
        <IconSearch />
        Search people, requests, records…
      </div>

      <div className="topbar-right">
        <div className="mini-clock"><span className="dot" /><span>{timeString} IST</span></div>
        <button
          className={'topbar-clock-btn' + (clockedIn ? ' on' : '')}
          onClick={handleClockToggle}
          disabled={shiftComplete}
          title={shiftComplete ? 'Shift complete' : clockLabel}
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
