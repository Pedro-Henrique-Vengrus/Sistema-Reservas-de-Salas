import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { api } from '../api/client';

export default function Layout() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [pendentes, setPendentes] = useState(0);

  async function carregarBadge() {
    try {
      const r = await api.get('/propostas/pendentes/count');
      setPendentes(r.count || 0);
    } catch (_) { /* silencioso */ }
  }

  useEffect(() => {
    carregarBadge();
    const t = setInterval(carregarBadge, 15000);
    return () => clearInterval(t);
  }, []);

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div>
      <div className="topbar">
        <span className="brand">🏫 CampusFlow</span>
        <div className="user">
          <span>{user?.nome}</span>
          <button onClick={handleLogout} title="Sair">⎋</button>
        </div>
      </div>

      <div className="tabs">
        <NavLink to="/agenda">📅 Agenda</NavLink>
        <NavLink to="/salas">🏛 Salas</NavLink>
        <NavLink to="/minhas-reservas">🔖 Minhas Reservas</NavLink>
        <NavLink to="/propostas">
          ✉ Propostas {pendentes > 0 && <span className="badge">{pendentes}</span>}
        </NavLink>
        {isAdmin && <NavLink to="/admin">⚙ Admin</NavLink>}
      </div>

      <Outlet />
    </div>
  );
}
