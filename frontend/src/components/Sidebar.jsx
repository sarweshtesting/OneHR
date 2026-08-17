import { useRef, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { roleLabel } from '../utils/roles';
import {
  IconGrid, IconClock, IconCalendar, IconOrgChart, IconPeople,
  IconPayroll, IconClientTracking, IconAdmin, IconAuditLog, IconChat,
  IconCalendarDays, IconStar, IconChevronDown, IconPanelToggle,
  IconSun, IconMoon, IconAuto,
} from './icons';

const THEME_OPTIONS = [
  { value: 'light', label: 'Light', icon: <IconSun /> },
  { value: 'dark', label: 'Dark', icon: <IconMoon /> },
  { value: 'auto', label: 'Auto', icon: <IconAuto /> },
];

function NavItem({ to, icon, children, badge }) {
  return (
    <NavLink to={to} className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')}>
      {icon}
      <span className="label">{children}</span>
      {badge > 0 && <span className="nav-badge">{badge}</span>}
    </NavLink>
  );
}

function InertNavItem({ icon, children }) {
  return (
    <a className="nav-item" href="#" onClick={(e) => e.preventDefault()} style={{ opacity: 0.55, cursor: 'default' }}>
      {icon}
      <span className="label">{children}</span>
    </a>
  );
}

/**
 * Collapsed by default; reveals its children on hover *or* click (click pins it open
 * so touch/keyboard users aren't stuck with a hover-only affordance). When the whole
 * sidebar is collapsed to its icon rail, children render as a flyout instead of an
 * inline accordion.
 */
function NavGroup({ label, icon, children }) {
  const [open, setOpen] = useState(false);
  const pinnedRef = useRef(false);
  const closeTimer = useRef(null);

  function openGroup() {
    clearTimeout(closeTimer.current);
    setOpen(true);
  }
  function scheduleClose() {
    closeTimer.current = setTimeout(() => {
      if (!pinnedRef.current) setOpen(false);
    }, 220);
  }
  function toggleClick() {
    pinnedRef.current = !pinnedRef.current;
    setOpen(pinnedRef.current);
  }

  return (
    <div className={'nav-group' + (open ? ' open' : '')} onMouseEnter={openGroup} onMouseLeave={scheduleClose}>
      <button type="button" className="nav-group-head" onClick={toggleClick} aria-expanded={open}>
        {icon}
        <span className="label">{label}</span>
        <span className="nav-group-chev"><IconChevronDown /></span>
      </button>
      <div className="nav-group-children"><div>{children}</div></div>
    </div>
  );
}

export default function Sidebar() {
  const { user, canAccessFinance } = useAuth();
  const { theme, setTheme } = useTheme();
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem('nexora_sidebar_collapsed') === '1');

  function toggleCollapsed() {
    setCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem('nexora_sidebar_collapsed', next ? '1' : '0');
      return next;
    });
  }

  return (
    <nav className={'sidebar' + (collapsed ? ' collapsed' : '')}>
      <div className="brand">
        <div className="brand-mark" />
        <div className="brand-name">NEX<span>ORA</span></div>
        <button type="button" className="sidebar-toggle" onClick={toggleCollapsed} title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}>
          <IconPanelToggle />
        </button>
      </div>

      <div className="nav-scroll">
        <NavItem to="/overview" icon={<IconGrid />}>Overview</NavItem>

        <NavGroup label="Organization" icon={<IconPeople />}>
          <NavItem to="/people" icon={<IconPeople />}>People</NavItem>
          <NavItem to="/org-hierarchy" icon={<IconOrgChart />}>Organisation</NavItem>
        </NavGroup>

        <NavGroup label="Planning" icon={<IconCalendarDays />}>
          <NavItem to="/leave" icon={<IconCalendar />}>Leave</NavItem>
          <NavItem to="/calendar" icon={<IconCalendarDays />}>Calendar</NavItem>
          <NavItem to="/appraisal" icon={<IconStar />}>Appraisal</NavItem>
        </NavGroup>

        <NavGroup label="Operations" icon={<IconClock />}>
          <NavItem to="/attendance" icon={<IconClock />}>Attendance</NavItem>
        </NavGroup>

        <NavGroup label="Finance &amp; Ops" icon={<IconPayroll />}>
          <NavItem to="/payslips" icon={<IconPayroll />}>Payslips</NavItem>
          {canAccessFinance
            ? <NavItem to="/finance" icon={<IconChat />}>Finance chat</NavItem>
            : <InertNavItem icon={<IconChat />}>Finance chat</InertNavItem>}
          <InertNavItem icon={<IconClientTracking />}>Client Tracking</InertNavItem>
        </NavGroup>

        <NavGroup label="Governance" icon={<IconAdmin />}>
          <InertNavItem icon={<IconAdmin />}>Admin</InertNavItem>
          <InertNavItem icon={<IconAuditLog />}>Audit Log</InertNavItem>
        </NavGroup>
      </div>

      <div className="sidebar-footer">
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <span className="role-pill">
            <span className="role-dot" />
            <span>Signed in as {roleLabel(user?.role)}</span>
          </span>
        </div>
        <div className="theme-switch" role="group" aria-label="Display theme">
          {THEME_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              className={'theme-switch-btn' + (theme === opt.value ? ' active' : '')}
              onClick={() => setTheme(opt.value)}
              title={opt.label}
              aria-pressed={theme === opt.value}
            >
              {opt.icon}
              <span className="label">{opt.label}</span>
            </button>
          ))}
        </div>
      </div>
    </nav>
  );
}
