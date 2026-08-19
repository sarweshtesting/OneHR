import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RequireManagerAccess() {
  const { isManager } = useAuth();
  if (!isManager) {
    return <Navigate to="/overview" replace />;
  }
  return <Outlet />;
}
