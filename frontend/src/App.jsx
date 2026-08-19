import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import RequireFinanceAccess from './components/RequireFinanceAccess';
import RequireManagePeopleAccess from './components/RequireManagePeopleAccess';
import RequireManagerAccess from './components/RequireManagerAccess';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import OverviewPage from './pages/OverviewPage';
import AttendancePage from './pages/AttendancePage';
import LeavePage from './pages/LeavePage';
import ProfilePage from './pages/ProfilePage';
import NotificationsPage from './pages/NotificationsPage';
import OrgHierarchyPage from './pages/OrgHierarchyPage';
import CalendarPage from './pages/CalendarPage';
import HelpPage from './pages/HelpPage';
import AppraisalPage from './pages/AppraisalPage';
import PeoplePage from './pages/PeoplePage';
import FinanceChatPage from './pages/FinanceChatPage';
import PayslipsPage from './pages/PayslipsPage';
import ClientTrackingPage from './pages/ClientTrackingPage';
import AdminPage from './pages/AdminPage';
import AuditLogPage from './pages/AuditLogPage';
import ExceptionDashboardPage from './pages/ExceptionDashboardPage';
import ServiceRequestsPage from './pages/ServiceRequestsPage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/overview" element={<OverviewPage />} />
          <Route path="/attendance" element={<AttendancePage />} />
          <Route path="/leave" element={<LeavePage />} />
          <Route path="/calendar" element={<CalendarPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/org-hierarchy" element={<OrgHierarchyPage />} />
          <Route path="/appraisal" element={<AppraisalPage />} />
          <Route path="/people" element={<PeoplePage />} />
          <Route path="/payslips" element={<PayslipsPage />} />
          <Route path="/client-tracking" element={<ClientTrackingPage />} />
          <Route element={<RequireManagerAccess />}>
            <Route path="/exceptions" element={<ExceptionDashboardPage />} />
          </Route>
          <Route path="/service-requests" element={<ServiceRequestsPage />} />
          <Route element={<RequireFinanceAccess />}>
            <Route path="/finance" element={<FinanceChatPage />} />
          </Route>
          <Route element={<RequireManagePeopleAccess />}>
            <Route path="/admin" element={<AdminPage />} />
            <Route path="/audit-log" element={<AuditLogPage />} />
          </Route>
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/help" element={<HelpPage />} />
          <Route path="/" element={<Navigate to="/overview" replace />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
