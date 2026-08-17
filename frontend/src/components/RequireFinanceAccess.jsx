import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RequireFinanceAccess() {
  const { canAccessFinance } = useAuth();
  if (!canAccessFinance) {
    return <Navigate to="/overview" replace />;
  }
  return <Outlet />;
}
