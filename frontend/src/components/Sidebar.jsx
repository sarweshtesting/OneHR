import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { roleLabel } from '../utils/roles';
import {
  IconGrid, IconClock, IconCalendar, IconOrgChart, IconPeople,
  IconPayroll, IconClientTracking, IconAdmin, IconAuditLog, IconChat,
  IconCalendarDays, IconStar, IconChevronDown, IconPanelToggle,
  IconWarningTriangle, IconHelp, IconTeamDirectory,
} from './icons';

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
 * Collapsed by default; reveals its children only on click (no hover-to-open —
 * hovering was too easy to trigger by accident while scrolling/moving the mouse
 * past the sidebar). When the whole sidebar is collapsed to its icon rail, children
 * render as a flyout instead of an inline accordion.
 */
function NavGroup({ label, icon, children }) {
  const [open, setOpen] = useState(false);

  function toggleClick() {
    setOpen((prev) => !prev);
  }

  return (
    <div className={'nav-group' + (open ? ' open' : '')}>
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
  const { user, canAccessFinance, canManagePeople, isManager } = useAuth();
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
          {isManager
            ? <NavItem to="/my-team" icon={<IconTeamDirectory />}>My Team</NavItem>
            : <InertNavItem icon={<IconTeamDirectory />}>My Team</InertNavItem>}
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
          {isManager
            ? <NavItem to="/exceptions" icon={<IconWarningTriangle />}>Exceptions</NavItem>
            : <InertNavItem icon={<IconWarningTriangle />}>Exceptions</InertNavItem>}
        </NavGroup>

        <NavGroup label="Finance &amp; Ops" icon={<IconPayroll />}>
          <NavItem to="/payslips" icon={<IconPayroll />}>Payslips</NavItem>
          {canAccessFinance
            ? <NavItem to="/finance" icon={<IconChat />}>Finance chat</NavItem>
            : <InertNavItem icon={<IconChat />}>Finance chat</InertNavItem>}
          <NavItem to="/client-tracking" icon={<IconClientTracking />}>Client Tracking</NavItem>
        </NavGroup>

        <NavGroup label="Governance" icon={<IconAdmin />}>
          <NavItem to="/service-requests" icon={<IconHelp />}>Service Requests</NavItem>
          {canManagePeople
            ? <NavItem to="/admin" icon={<IconAdmin />}>Admin</NavItem>
            : <InertNavItem icon={<IconAdmin />}>Admin</InertNavItem>}
          {canManagePeople
            ? <NavItem to="/audit-log" icon={<IconAuditLog />}>Audit Log</NavItem>
            : <InertNavItem icon={<IconAuditLog />}>Audit Log</InertNavItem>}
        </NavGroup>
      </div>

      <div className="sidebar-footer">
        <div className="role-name">{roleLabel(user?.role)}</div>
      </div>
    </nav>
  );
}
