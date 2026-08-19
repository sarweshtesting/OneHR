import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RequireManagePeopleAccess() {
  const { canManagePeople } = useAuth();
  if (!canManagePeople) {
    return <Navigate to="/overview" replace />;
  }
  return <Outlet />;
}
