import { Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';

/**
 * Guarda de rota. `roles` restringe a perfis especificos;
 * `administrativo` exige ADMIN ou REITOR.
 */
export default function ProtectedRoute({ children, roles, administrativo = false }) {
  const { user, ehAdministrativo } = useAuth();

  if (!user) return <Navigate to="/login" replace />;
  if (administrativo && !ehAdministrativo) return <Navigate to="/" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;

  return children;
}
