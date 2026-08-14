import { useAuth } from '../context/AuthContext';
import { useClock } from '../hooks/useClock';
import { IconSearch, IconBell } from './icons';

function initialsOf(name) {
  return name.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');
}

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
        <button className="icon-btn" title="Notifications">
          <IconBell />
          <span className="ping" />
        </button>
        <div className="user-chip">
          <div className="avatar-circle">{user ? initialsOf(user.name) : ''}</div>
          <div className="user-meta">
            <div className="name">{user?.name}</div>
            <div className="role">{user?.role.replace('_', ' ')}{currentOrgName ? ` · ${currentOrgName}` : ''}</div>
          </div>
        </div>
      </div>
    </header>
  );
}
