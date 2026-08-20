import { useCallback, useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { api } from '../api/client';
import { iniciais } from '../lib/format';
import NotificacoesPopover from './NotificacoesPopover';

const ROTULOS = {
  '': 'Painel', agenda: 'Agenda', ambientes: 'Ambientes', 'minhas-reservas': 'Minhas reservas',
  trocas: 'Trocas de sala', admin: 'Administração', usuarios: 'Usuários', salas: 'Ambientes',
  cursos: 'Cursos', moderacao: 'Moderação', 'periodo-grade': 'Período da grade', relatorios: 'Relatórios',
};

/**
 * Casca da aplicacao: sidebar lateral colapsavel + cabecalho com acoes rapidas.
 * As secoes visiveis dependem do perfil (solicitante x administrativo).
 */
export default function AppShell() {
  const { user, ehAdministrativo, ehSolicitante, logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();

  const [colapsada, setColapsada] = useState(() => localStorage.getItem('cf_sidebar') === 'collapsed');
  const [contadores, setContadores] = useState({ trocas: 0, moderacao: 0, avisos: 0 });

  const carregarContadores = useCallback(async () => {
    const proximo = { trocas: 0, moderacao: 0, avisos: 0 };
    const pedidos = [
      api.get('/notificacoes/nao-lidas/count').then((r) => { proximo.avisos = r.count; }).catch(() => {}),
    ];
    if (ehSolicitante) {
      pedidos.push(api.get('/propostas/pendentes/count').then((r) => { proximo.trocas = r.count; }).catch(() => {}));
    }
    if (ehAdministrativo) {
      pedidos.push(api.get('/reservas/moderacao/count').then((r) => { proximo.moderacao = r.count; }).catch(() => {}));
    }
    await Promise.all(pedidos);
    setContadores(proximo);
  }, [ehAdministrativo, ehSolicitante]);

  useEffect(() => {
    carregarContadores();
    const t = setInterval(carregarContadores, 30000);
    return () => clearInterval(t);
  }, [carregarContadores]);

  function alternarSidebar() {
    setColapsada((c) => {
      localStorage.setItem('cf_sidebar', !c ? 'collapsed' : 'expanded');
      return !c;
    });
  }

  function sair() {
    logout();
    navigate('/login');
  }

  const trilha = pathname.split('/').filter(Boolean);
  const atual = ROTULOS[trilha[trilha.length - 1]] || ROTULOS[''] || 'Painel';

  return (
    <div className="shell">
      <nav className={`sidebar ${colapsada ? 'collapsed' : ''}`}>
        <div className="sidebar-brand">
          <span className="mark" aria-hidden>🏫</span>
          {!colapsada && <span className="name">CampusFlow</span>}
        </div>

        <div className="sidebar-nav">
          {ehSolicitante && (
            <>
              <div className="sidebar-section">Solicitante</div>
              <Item to="/" icone="▦" rotulo="Painel" fim />
              <Item to="/agenda" icone="▤" rotulo="Agenda" />
              <Item to="/ambientes" icone="🏛" rotulo="Ambientes" />
              <Item to="/minhas-reservas" icone="📋" rotulo="Minhas reservas" />
              <Item to="/trocas" icone="⇄" rotulo="Trocas de sala" contador={contadores.trocas} />
            </>
          )}

          {ehAdministrativo && (
            <>
              <div className="sidebar-section">Administração</div>
              {!ehSolicitante && <Item to="/" icone="▦" rotulo="Painel" fim />}
              <Item to="/admin/moderacao" icone="⚖" rotulo="Moderação" contador={contadores.moderacao} />
              <Item to="/admin/usuarios" icone="👤" rotulo="Usuários" />
              <Item to="/admin/salas" icone="🏛" rotulo="Ambientes" />
              <Item to="/admin/cursos" icone="🎓" rotulo="Cursos" />
              <Item to="/admin/periodo-grade" icone="🗓" rotulo="Período da grade" />
              <Item to="/admin/relatorios" icone="📊" rotulo="Relatórios" />
            </>
          )}
        </div>

        <div className="sidebar-footer">
          <button className="sidebar-toggle" onClick={alternarSidebar}
            title={colapsada ? 'Expandir menu' : 'Recolher menu'}>
            <span className="icon" aria-hidden>{colapsada ? '»' : '«'}</span>
            {!colapsada && <span>Recolher menu</span>}
          </button>
        </div>
      </nav>

      <div className="main">
        <header className="appbar">
          <div className="crumbs">
            <span>CampusFlow</span>
            <span aria-hidden>/</span>
            <span className="current">{atual}</span>
          </div>

          <div className="appbar-actions">
            {ehSolicitante && (
              <button className="btn btn-sm" onClick={() => navigate('/agenda')}>+ Nova reserva</button>
            )}
            <NotificacoesPopover naoLidas={contadores.avisos} aoAtualizar={carregarContadores} />
            <div className="user-chip">
              <span className="avatar" aria-hidden>{iniciais(user?.nome)}</span>
              <span className="meta">
                <strong>{user?.nome}</strong>
                <span>{user?.role}</span>
              </span>
              <button className="icon-btn" onClick={sair} title="Sair" aria-label="Sair">⏻</button>
            </div>
          </div>
        </header>

        <main className="content">
          <Outlet context={{ recarregarContadores: carregarContadores }} />
        </main>
      </div>
    </div>
  );
}

function Item({ to, icone, rotulo, contador = 0, fim = false }) {
  return (
    <NavLink to={to} end={fim} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`} title={rotulo}>
      <span className="icon" aria-hidden>{icone}</span>
      <span className="label">{rotulo}</span>
      {contador > 0 && <span className="count">{contador}</span>}
    </NavLink>
  );
}
