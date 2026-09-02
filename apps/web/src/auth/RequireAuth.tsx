import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getAccessToken } from './session';

export function RequireAuth() {
  const location = useLocation();

  if (!getAccessToken()) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}

