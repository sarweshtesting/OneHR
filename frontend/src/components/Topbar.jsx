import { useAuth } from '../context/AuthContext';
import { useClock } from '../hooks/useClock';
import { IconSearch } from './icons';
import NotificationBell from './NotificationBell';
import UserMenu from './UserMenu';

export default function Topbar() {
  const { user, organizations, selectedOrgId, selectOrg } = useAuth();
  const { timeString } = useClock();

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
        <NotificationBell />
        <UserMenu orgName={currentOrgName} />
      </div>
    </header>
  );
}
