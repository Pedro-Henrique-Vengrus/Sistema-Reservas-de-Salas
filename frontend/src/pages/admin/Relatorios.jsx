import { useCallback, useEffect, useState } from 'react';
import { api, qs } from '../../api/client';
import { useToast } from '../../components/ui/ToastProvider';
import {
  EmptyState, Field, Notice, PageHeader, StatusBadge,
} from '../../components/ui/primitives';
import {
  dataBr, diaDaSemana, hhmm, hojeIso, somaDias, STATUS_RESERVA, TIPOS_RESERVA, TURNOS,
} from '../../lib/format';

const FILTROS_INICIAIS = () => ({
  inicio: somaDias(hojeIso(), -30),
  fim: somaDias(hojeIso(), 60),
  cursoId: '', salaId: '', usuarioId: '', status: '', tipo: '', turno: '',
});

const ORDENAVEIS = ['data', 'salaNome', 'solicitanteNome', 'status'];

/** Relatorio tabular das reservas: filtros, paginacao server-side e exportacao CSV. */
export default function Relatorios() {
  const toast = useToast();
  const [filtros, setFiltros] = useState(FILTROS_INICIAIS);
  const [pagina, setPagina] = useState(0);
  const [ordem, setOrdem] = useState({ chave: 'data', dir: 'desc' });
  const [resultado, setResultado] = useState(null);
  const [apoio, setApoio] = useState({ cursos: [], salas: [], usuarios: [] });
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    Promise.all([
      api.get('/cursos'),
      api.get('/salas'),
      api.get('/usuarios'),
    ])
      .then(([cursos, salas, usuarios]) => setApoio({ cursos, salas, usuarios }))
      .catch(() => {});
  }, []);

  const buscar = useCallback(() => {
    setCarregando(true);
    api.get(`/reservas${qs({ ...filtros, pagina, tamanho: 20 })}`)
      .then(setResultado)
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }, [filtros, pagina]);

  useEffect(() => { buscar(); }, [buscar]);

  async function exportar() {
    try {
      await api.download(`/relatorios/reservas.csv${qs(filtros)}`, 'reservas-campusflow.csv');
      toast.sucesso('Relatório exportado.');
    } catch (e) { toast.erro(e.message); }
  }

  function mudarFiltro(campo, valor) {
    setFiltros((f) => ({ ...f, [campo]: valor }));
    setPagina(0);
  }

  function alternarOrdem(chave) {
    setOrdem((o) => (o.chave === chave
      ? { chave, dir: o.dir === 'asc' ? 'desc' : 'asc' }
      : { chave, dir: 'asc' }));
  }

  // A API pagina e devolve por data desc; a ordenacao das colunas atua sobre a pagina carregada.
  const linhas = [...(resultado?.content || [])].sort((a, b) => {
    const va = a[ordem.chave]; const vb = b[ordem.chave];
    const cmp = String(va).localeCompare(String(vb), 'pt-BR', { numeric: true });
    return ordem.dir === 'asc' ? cmp : -cmp;
  });

  const ativos = Object.entries(filtros).filter(([k, v]) => v && !['inicio', 'fim'].includes(k));

  return (
    <>
      <PageHeader titulo="Relatórios de reservas"
        descricao="Consulta tabular por período, curso, ambiente, solicitante e status — com exportação em CSV."
        acoes={(
          <>
            <button className="btn btn-secondary" onClick={() => { setFiltros(FILTROS_INICIAIS()); setPagina(0); }}>
              Limpar filtros
            </button>
            <button className="btn" onClick={exportar}>⤓ Exportar CSV</button>
          </>
        )} />

      {erro && <div className="mb-4"><Notice tom="danger">{erro}</Notice></div>}

      <div className="filterbar">
        <Field label="De">
          <input className="input" type="date" value={filtros.inicio}
            onChange={(e) => mudarFiltro('inicio', e.target.value)} />
        </Field>
        <Field label="Até">
          <input className="input" type="date" value={filtros.fim}
            onChange={(e) => mudarFiltro('fim', e.target.value)} />
        </Field>
        <Field label="Curso">
          <select className="select" value={filtros.cursoId} onChange={(e) => mudarFiltro('cursoId', e.target.value)}>
            <option value="">Todos</option>
            {apoio.cursos.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
          </select>
        </Field>
        <Field label="Ambiente">
          <select className="select" value={filtros.salaId} onChange={(e) => mudarFiltro('salaId', e.target.value)}>
            <option value="">Todos</option>
            {apoio.salas.map((s) => <option key={s.id} value={s.id}>{s.nome}</option>)}
          </select>
        </Field>
        <Field label="Solicitante">
          <select className="select" value={filtros.usuarioId} onChange={(e) => mudarFiltro('usuarioId', e.target.value)}>
            <option value="">Todos</option>
            {apoio.usuarios.map((u) => <option key={u.id} value={u.id}>{u.nome}</option>)}
          </select>
        </Field>
        <Field label="Status">
          <select className="select" value={filtros.status} onChange={(e) => mudarFiltro('status', e.target.value)}>
            <option value="">Todos</option>
            {STATUS_RESERVA.map((s) => <option key={s.valor} value={s.valor}>{s.rotulo}</option>)}
          </select>
        </Field>
        <Field label="Modo">
          <select className="select" value={filtros.tipo} onChange={(e) => mudarFiltro('tipo', e.target.value)}>
            <option value="">Todos</option>
            {TIPOS_RESERVA.map((t) => <option key={t.valor} value={t.valor}>{t.rotulo}</option>)}
          </select>
        </Field>
        <Field label="Turno">
          <select className="select" value={filtros.turno} onChange={(e) => mudarFiltro('turno', e.target.value)}>
            <option value="">Todos</option>
            {TURNOS.map((t) => <option key={t.valor} value={t.valor}>{t.rotulo}</option>)}
          </select>
        </Field>
      </div>

      {ativos.length > 0 && (
        <div className="chips mb-4">
          <span className="text-sm text-muted">Filtros ativos:</span>
          {ativos.map(([campo, valor]) => (
            <span className="chip" key={campo}>
              {rotuloFiltro(campo, valor, apoio)}
              <button onClick={() => mudarFiltro(campo, '')} aria-label="Remover filtro">✕</button>
            </span>
          ))}
        </div>
      )}

      <div className="table-wrap">
        <div className="table-toolbar">
          <strong className="text-md">
            {resultado ? `${resultado.totalElements} reserva(s) no período` : 'Carregando…'}
          </strong>
          <span className="text-sm text-muted">
            {dataBr(filtros.inicio)} → {dataBr(filtros.fim)}
          </span>
        </div>

        <div className="table-scroll">
          {carregando
            ? <div style={{ padding: 24 }}><div className="skeleton" style={{ height: 160 }} /></div>
            : (
              <table className="data">
                <thead>
                  <tr>
                    <Th chave="data" ordem={ordem} onClick={alternarOrdem}>Data</Th>
                    <th>Horário</th>
                    <th>Turno</th>
                    <Th chave="salaNome" ordem={ordem} onClick={alternarOrdem}>Ambiente</Th>
                    <Th chave="solicitanteNome" ordem={ordem} onClick={alternarOrdem}>Solicitante</Th>
                    <th>Modo</th>
                    <Th chave="status" ordem={ordem} onClick={alternarOrdem}>Status</Th>
                  </tr>
                </thead>
                <tbody>
                  {linhas.map((r) => (
                    <tr key={r.id}>
                      <td className="nowrap">
                        {dataBr(r.data)} <span className="text-muted text-sm">{diaDaSemana(r.data, true)}</span>
                      </td>
                      <td className="nowrap">{hhmm(r.horaInicio)}–{hhmm(r.horaFim)}</td>
                      <td><StatusBadge valor={r.turno} /></td>
                      <td>
                        <strong>{r.salaNome}</strong>
                        {r.salaCodigo && <span className="text-muted text-sm"> · {r.salaCodigo}</span>}
                      </td>
                      <td>{r.solicitanteNome}</td>
                      <td><StatusBadge valor={r.tipoReserva} /></td>
                      <td><StatusBadge valor={r.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}

          {!carregando && linhas.length === 0 && (
            <EmptyState icone="📊" titulo="Nenhuma reserva no recorte"
              descricao="Amplie o período ou remova alguns filtros." />
          )}
        </div>

        <div className="table-foot">
          <span>
            {resultado && resultado.totalElements > 0
              ? `Página ${resultado.number + 1} de ${resultado.totalPages} · ${resultado.totalElements} registro(s)`
              : 'Nenhum registro'}
          </span>
          {resultado && resultado.totalPages > 1 && (
            <div className="pager">
              <button className="btn btn-secondary btn-sm" disabled={resultado.first}
                onClick={() => setPagina(pagina - 1)}>← Anterior</button>
              <button className="btn btn-secondary btn-sm" disabled={resultado.last}
                onClick={() => setPagina(pagina + 1)}>Próxima →</button>
            </div>
          )}
        </div>
      </div>
    </>
  );
}

function Th({ chave, ordem, onClick, children }) {
  return (
    <th className={ORDENAVEIS.includes(chave) ? 'sortable' : ''} onClick={() => onClick(chave)}>
      {children}
      {ordem.chave === chave && <span className="sort">{ordem.dir === 'asc' ? '▲' : '▼'}</span>}
    </th>
  );
}

function rotuloFiltro(campo, valor, apoio) {
  const achar = (lista, id) => lista.find((x) => String(x.id) === String(id));
  switch (campo) {
    case 'cursoId': return `Curso: ${achar(apoio.cursos, valor)?.nome || valor}`;
    case 'salaId': return `Ambiente: ${achar(apoio.salas, valor)?.nome || valor}`;
    case 'usuarioId': return `Solicitante: ${achar(apoio.usuarios, valor)?.nome || valor}`;
    case 'status': return `Status: ${STATUS_RESERVA.find((s) => s.valor === valor)?.rotulo || valor}`;
    case 'tipo': return `Modo: ${TIPOS_RESERVA.find((t) => t.valor === valor)?.rotulo || valor}`;
    case 'turno': return `Turno: ${TURNOS.find((t) => t.valor === valor)?.rotulo || valor}`;
    default: return `${campo}: ${valor}`;
  }
}
