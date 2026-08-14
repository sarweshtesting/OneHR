import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  IconGrid, IconClock, IconCalendar, IconOrgChart, IconPeople,
  IconPayroll, IconClientTracking, IconChat, IconAdmin, IconAuditLog,
} from './icons';

function NavItem({ to, icon, children }) {
  return (
    <NavLink to={to} className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')}>
      {icon}
      {children}
    </NavLink>
  );
}

function InertNavItem({ icon, children }) {
  return (
    <a className="nav-item" href="#" onClick={(e) => e.preventDefault()} style={{ opacity: 0.55, cursor: 'default' }}>
      {icon}
      {children}
    </a>
  );
}

export default function Sidebar() {
  const { user, logout } = useAuth();

  return (
    <nav className="sidebar">
      <div className="brand">
        <div className="brand-mark" />
        <div className="brand-name">nForce<span>HQ</span></div>
      </div>

      <div className="nav-group-label">Workspace</div>
      <NavItem to="/overview" icon={<IconGrid />}>Overview</NavItem>
      <NavItem to="/attendance" icon={<IconClock />}>Attendance</NavItem>
      <NavItem to="/leave" icon={<IconCalendar />}>Leave</NavItem>
      <InertNavItem icon={<IconOrgChart />}>Org Chart</InertNavItem>
      <InertNavItem icon={<IconPeople />}>People</InertNavItem>

      <div className="nav-group-label">Finance &amp; Ops</div>
      <InertNavItem icon={<IconPayroll />}>Payroll</InertNavItem>
      <InertNavItem icon={<IconClientTracking />}>Client Tracking</InertNavItem>
      <InertNavItem icon={<IconChat />}>Chat</InertNavItem>

      <div className="nav-group-label">Governance</div>
      <InertNavItem icon={<IconAdmin />}>Admin</InertNavItem>
      <InertNavItem icon={<IconAuditLog />}>Audit Log</InertNavItem>

      <div className="sidebar-footer">
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <span className="role-pill">
            <span className="role-dot" />
            <span>Signed in as {user?.role.replace('_', ' ')}</span>
          </span>
          <button className="logout-link" onClick={logout}>Log out</button>
        </div>
      </div>
    </nav>
  );
}
