import { Outlet } from 'react-router-dom';
import { AttendanceProvider } from '../context/AttendanceContext';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

export default function Layout() {
  return (
    <AttendanceProvider>
      <div className="app-shell">
        <Sidebar />
        <div className="main">
          <Topbar />
          <main className="content">
            <Outlet />
          </main>
        </div>
      </div>
    </AttendanceProvider>
  );
}
