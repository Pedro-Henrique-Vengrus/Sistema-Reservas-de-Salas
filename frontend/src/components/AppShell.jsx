import { useCallback, useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { api } from '../api/client';
import { iniciais } from '../lib/format';
import NotificacoesPopover from './NotificacoesPopover';
import Logo from './ui/Logo';
import Marca from './ui/Marca';
import Preferencias from './Preferencias';
import Icone from './ui/Icone';

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
  const [preferencias, setPreferencias] = useState(false);

  const carregarContadores = useCallback(async () => {
    const proximo = { trocas: 0, moderacao: 0, avisos: 0 };
    const pedidos = [
      api.get('/notificacoes/nao-lidas/count').then((r) => { proximo.avisos = r.count; }).catch(() => {}),
    ];
    if (ehSolicitante) {
      pedidos.push(api.get('/propostas/pendentes/count').then((r) => { proximo.trocas = r.count; }).catch(() => {}));
    }
    if (ehAdministrativo) {
      // A fila de moderacao soma solicitacoes de ultima hora e trocas fora do dia/turno
      pedidos.push(api.get('/reservas/moderacao/count').then((r) => { proximo.moderacao += r.count; }).catch(() => {}));
      pedidos.push(api.get('/propostas/moderacao/count').then((r) => { proximo.moderacao += r.count; }).catch(() => {}));
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
          {/* Recolhida nao cabe o logotipo com o nome: fica so o simbolo */}
          {colapsada
            ? <span className="mark"><Logo tamanho={26} /></span>
            : <Marca altura={28} />}
        </div>

        <div className="sidebar-nav">
          {ehSolicitante && (
            <>
              <div className="sidebar-section">Solicitante</div>
              <Item to="/" icone="painel" rotulo="Painel" fim />
              <Item to="/agenda" icone="agenda" rotulo="Agenda" />
              <Item to="/ambientes" icone="ambiente" rotulo="Ambientes" />
              <Item to="/minhas-reservas" icone="reservas" rotulo="Minhas reservas" />
              <Item to="/trocas" icone="troca" rotulo="Trocas de sala" contador={contadores.trocas} />
            </>
          )}

          {ehAdministrativo && (
            <>
              <div className="sidebar-section">Administração</div>
              {!ehSolicitante && <Item to="/" icone="painel" rotulo="Painel" fim />}
              <Item to="/admin/moderacao" icone="moderacao" rotulo="Moderação" contador={contadores.moderacao} />
              <Item to="/admin/usuarios" icone="usuarios" rotulo="Usuários" />
              <Item to="/admin/salas" icone="ambiente" rotulo="Ambientes" />
              <Item to="/admin/cursos" icone="cursos" rotulo="Cursos" />
              <Item to="/admin/periodo-grade" icone="periodo" rotulo="Período da grade" />
              <Item to="/admin/relatorios" icone="relatorios" rotulo="Relatórios" />
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
            <button className="icon-btn" onClick={() => setPreferencias(true)}
              title="Preferências" aria-label="Preferências"><Icone nome="preferencias" tamanho={17} /></button>
            <div className="user-chip">
              <span className="avatar" aria-hidden>{iniciais(user?.nome)}</span>
              <span className="meta">
                <strong>{user?.nome}</strong>
                <span>{user?.role}</span>
              </span>
              <button className="icon-btn" onClick={sair} title="Sair" aria-label="Sair"><Icone nome="sair" tamanho={17} /></button>
            </div>
          </div>
        </header>

        <main className="content">
          <Outlet context={{ recarregarContadores: carregarContadores }} />
        </main>

        {preferencias && <Preferencias onFechar={() => setPreferencias(false)} />}
      </div>
    </div>
  );
}

function Item({ to, icone, rotulo, contador = 0, fim = false }) {
  return (
    <NavLink to={to} end={fim} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`} title={rotulo}>
      <span className="icon"><Icone nome={icone} /></span>
      <span className="label">{rotulo}</span>
      {contador > 0 && <span className="count">{contador}</span>}
    </NavLink>
  );
}
