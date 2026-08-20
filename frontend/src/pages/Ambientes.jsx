import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, qs } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import DataTable from '../components/ui/DataTable';
import { EmptyState, Field, Notice, PageHeader } from '../components/ui/primitives';

/** Catalogo dos ambientes visiveis ao solicitante, em tabela filtravel. */
export default function Ambientes() {
  const { user, semCurso } = useAuth();
  const [salas, setSalas] = useState([]);
  const [tipos, setTipos] = useState([]);
  const [filtros, setFiltros] = useState({ tipo: '', cursoId: '', capacidadeMinima: '' });
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(true);

  useEffect(() => { api.get('/salas/tipos').then(setTipos).catch(() => {}); }, []);

  useEffect(() => {
    setCarregando(true);
    api.get(`/salas${qs({ status: 'ATIVO', ...filtros })}`)
      .then(setSalas)
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }, [filtros]);

  const cursos = user?.cursos || [];

  const colunas = [
    { chave: 'nome', titulo: 'Ambiente', largura: 240,
      render: (s) => (
        <div>
          <strong>{s.nome}</strong>
          {s.codigo && <span className="text-muted text-sm"> · {s.codigo}</span>}
          <div className="text-sm text-muted">{s.andar}</div>
        </div>
      ) },
    { chave: 'tipoRotulo', titulo: 'Tipo' },
    { chave: 'capacidade', titulo: 'Capacidade', render: (s) => `${s.capacidade} lugares` },
    { chave: 'cursos', titulo: 'Cursos com acesso', ordenavel: false,
      valor: (s) => s.cursos.map((c) => c.nome).join(' '),
      render: (s) => s.cursos.map((c) => <span className="tag" key={c.id}>{c.sigla || c.nome}</span>) },
    { chave: 'acoes', titulo: '', ordenavel: false, alinhar: 'right',
      render: () => <Link className="btn btn-secondary btn-sm" to="/agenda">Ver na agenda</Link> },
  ];

  return (
    <>
      <PageHeader titulo="Ambientes"
        descricao="Salas e laboratórios vinculados aos cursos do seu perfil."
        acoes={<Link className="btn" to="/agenda">Abrir agenda</Link>} />

      {semCurso && (
        <div className="mb-4">
          <Notice tom="warn">
            Seu perfil não está vinculado a nenhum curso — por isso nenhum ambiente é listado.
          </Notice>
        </div>
      )}
      {erro && <div className="mb-4"><Notice tom="danger">{erro}</Notice></div>}

      <div className="filterbar">
        <Field label="Tipo de ambiente" className="field">
          <select className="select" value={filtros.tipo}
            onChange={(e) => setFiltros({ ...filtros, tipo: e.target.value })}>
            <option value="">Todos</option>
            {tipos.map((t) => <option key={t.valor} value={t.valor}>{t.rotulo}</option>)}
          </select>
        </Field>
        {cursos.length > 1 && (
          <Field label="Curso">
            <select className="select" value={filtros.cursoId}
              onChange={(e) => setFiltros({ ...filtros, cursoId: e.target.value })}>
              <option value="">Todos os meus cursos</option>
              {cursos.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
            </select>
          </Field>
        )}
        <Field label="Capacidade mínima">
          <input className="input" type="number" min="0" placeholder="Ex: 40" value={filtros.capacidadeMinima}
            onChange={(e) => setFiltros({ ...filtros, capacidadeMinima: e.target.value })} />
        </Field>
        <div className="spacer" />
        <button className="btn btn-secondary"
          onClick={() => setFiltros({ tipo: '', cursoId: '', capacidadeMinima: '' })}>
          Limpar filtros
        </button>
      </div>

      {carregando
        ? <div className="panel"><div className="skeleton" style={{ height: 180 }} /></div>
        : (
          <DataTable colunas={colunas} dados={salas} buscaPlaceholder="Buscar por nome ou código…"
            vazio={<EmptyState icone="🏛" titulo="Nenhum ambiente encontrado"
              descricao="Ajuste os filtros ou confirme com a administração os cursos vinculados ao seu perfil." />} />
        )}
    </>
  );
}
