import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { roleLabel } from '../utils/roles';
import { IconHelp, IconLogout, IconUser, IconSun, IconMoon, IconAuto } from './icons';
import AvatarCircle from './AvatarCircle';

const THEME_OPTIONS = [
  { value: 'light', label: 'Light', icon: <IconSun /> },
  { value: 'dark', label: 'Dark', icon: <IconMoon /> },
  { value: 'auto', label: 'Auto', icon: <IconAuto /> },
];

function initialsOf(name) {
  return name.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');
}

export default function UserMenu({ orgName }) {
  const { user, logout } = useAuth();
  const { theme, setTheme } = useTheme();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const closeTimer = useRef(null);

  function openMenu() {
    clearTimeout(closeTimer.current);
    setOpen(true);
  }
  function scheduleClose() {
    closeTimer.current = setTimeout(() => setOpen(false), 150);
  }

  function goToProfile() {
    setOpen(false);
    navigate('/profile');
  }
  function goToHelp() {
    setOpen(false);
    navigate('/help');
  }

  return (
    <div className="header-dropdown-anchor user-chip" onMouseEnter={openMenu} onMouseLeave={scheduleClose}>
      <AvatarCircle photoUrl={user?.avatarPhotoDataUri} initials={user ? initialsOf(user.name) : ''} />
      {open && (
        <div className="header-dropdown">
          <div className="header-dropdown-head">
            <AvatarCircle photoUrl={user?.avatarPhotoDataUri} initials={user ? initialsOf(user.name) : ''} />
            <div>
              <div className="name">{user?.name}</div>
              <div className="sub">{roleLabel(user?.role)}{orgName ? ` · ${orgName}` : ''}</div>
            </div>
          </div>
          <div className="header-dropdown-theme" role="group" aria-label="Display theme">
            {THEME_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                type="button"
                className={'header-dropdown-theme-btn' + (theme === opt.value ? ' active' : '')}
                onClick={() => setTheme(opt.value)}
                title={opt.label}
                aria-pressed={theme === opt.value}
              >
                {opt.icon}
              </button>
            ))}
          </div>
          <div className="header-dropdown-divider" />
          <button className="header-dropdown-item" onClick={goToProfile}><IconUser />My Profile</button>
          <button className="header-dropdown-item" onClick={goToHelp}><IconHelp />Help &amp; Guidance</button>
          <div className="header-dropdown-divider" />
          <button className="header-dropdown-item danger" onClick={logout}><IconLogout />Sign Out</button>
        </div>
      )}
    </div>
  );
}
