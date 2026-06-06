import { Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';

// Guard de rota: exige auth; se requireAdmin, exige ADMIN/GESTOR
export default function ProtectedRoute({ children, requireAdmin = false }) {
  const { user, isAdmin } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (requireAdmin && !isAdmin) return <Navigate to="/agenda" replace />;
  return children;
}
