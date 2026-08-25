import { useCallback, useEffect, useState } from 'react';
import { api, qs } from '../../api/client';
import DataTable from '../../components/ui/DataTable';
import { useToast } from '../../components/ui/ToastProvider';
import {
  ConfirmDialog, Drawer, EmptyState, Field, Notice, PageHeader, StatusBadge,
} from '../../components/ui/primitives';
import DialogoExclusao from '../../components/admin/DialogoExclusao';

const VAZIO = { nome: '', codigo: '', tipo: 'SALA_AULA', capacidade: 40, andar: '', cursoIds: [] };

/** CRUD de ambientes: vinculo N:N com cursos e ciclo de vida ativo -> inativo -> exclusao. */
export default function Salas() {
  const toast = useToast();
  const [salas, setSalas] = useState([]);
  const [cursos, setCursos] = useState([]);
  const [tipos, setTipos] = useState([]);
  const [filtros, setFiltros] = useState({ status: '', tipo: '', cursoId: '' });
  const [form, setForm] = useState(null);
  const [acao, setAcao] = useState(null);      // apenas reativacao
  const [excluindo, setExcluindo] = useState(null);
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(true);
  const [ocupado, setOcupado] = useState(false);

  const carregar = useCallback(() => {
    setCarregando(true);
    Promise.all([
      api.get(`/salas${qs(filtros)}`),
      api.get('/cursos?status=ATIVO'),
      api.get('/salas/tipos'),
    ])
      .then(([s, c, t]) => { setSalas(s); setCursos(c); setTipos(t); })
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }, [filtros]);

  useEffect(() => { carregar(); }, [carregar]);

  async function salvar() {
    setOcupado(true);
    try {
      const payload = { ...form, capacidade: Number(form.capacidade) };
      if (form.id) await api.put(`/salas/${form.id}`, payload);
      else await api.post('/salas', payload);
      toast.sucesso(form.id ? 'Ambiente atualizado.' : 'Ambiente cadastrado.');
      setForm(null);
      carregar();
    } catch (e) { toast.erro(e.message); } finally { setOcupado(false); }
  }

  async function reativar() {
    setOcupado(true);
    try {
      await api.post(`/salas/${acao.sala.id}/reativar`);
      toast.sucesso('Ambiente reativado.');
      setAcao(null);
      carregar();
    } catch (e) { toast.erro(e.message); } finally { setOcupado(false); }
  }

  const colunas = [
    { chave: 'nome', titulo: 'Ambiente',
      render: (s) => (
        <div>
          <strong>{s.nome}</strong>
          {s.codigo && <span className="text-muted text-sm"> · {s.codigo}</span>}
          <div className="text-sm text-muted">{s.andar || '—'}</div>
        </div>
      ) },
    { chave: 'tipoRotulo', titulo: 'Tipo' },
    { chave: 'capacidade', titulo: 'Capacidade', alinhar: 'left', render: (s) => `${s.capacidade}` },
    { chave: 'cursos', titulo: 'Cursos com acesso', ordenavel: false,
      valor: (s) => s.cursos.map((c) => c.nome).join(' '),
      render: (s) => (s.cursos.length
        ? s.cursos.map((c) => <span className="tag" key={c.id}>{c.sigla || c.nome}</span>)
        : <span className="text-muted text-sm">Nenhum — invisível para todos</span>) },
    { chave: 'status', titulo: 'Status', render: (s) => <StatusBadge valor={s.status} /> },
    { chave: 'acoes', titulo: '', ordenavel: false, alinhar: 'right',
      render: (s) => (
        <>
          <button className="btn btn-secondary btn-sm"
            onClick={() => setForm({
              id: s.id, nome: s.nome, codigo: s.codigo || '', tipo: s.tipo,
              capacidade: s.capacidade, andar: s.andar || '', cursoIds: s.cursos.map((c) => c.id),
            })}>Editar</button>
          {s.status === 'INATIVO' && (
            <button className="btn btn-secondary btn-sm"
              onClick={() => setAcao({ sala: s, tipo: 'reativar' })}>Reativar</button>
          )}
          <button className="btn btn-danger btn-sm" onClick={() => setExcluindo(s)}>Excluir</button>
        </>
      ) },
  ];

  return (
    <>
      <PageHeader titulo="Ambientes"
        descricao="Salas e laboratórios do campus. Os cursos vinculados definem quem enxerga e pode reservar cada ambiente."
        acoes={<button className="btn" onClick={() => setForm({ ...VAZIO })}>+ Novo ambiente</button>} />

      {erro && <div className="mb-4"><Notice tom="danger">{erro}</Notice></div>}

      <div className="filterbar">
        <Field label="Status">
          <select className="select" value={filtros.status}
            onChange={(e) => setFiltros({ ...filtros, status: e.target.value })}>
            <option value="">Todos</option>
            <option value="ATIVO">Ativos</option>
            <option value="INATIVO">Inativos</option>
          </select>
        </Field>
        <Field label="Tipo">
          <select className="select" value={filtros.tipo}
            onChange={(e) => setFiltros({ ...filtros, tipo: e.target.value })}>
            <option value="">Todos</option>
            {tipos.map((t) => <option key={t.valor} value={t.valor}>{t.rotulo}</option>)}
          </select>
        </Field>
        <Field label="Curso">
          <select className="select" value={filtros.cursoId}
            onChange={(e) => setFiltros({ ...filtros, cursoId: e.target.value })}>
            <option value="">Todos</option>
            {cursos.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
          </select>
        </Field>
        <div className="spacer" />
        <button className="btn btn-secondary"
          onClick={() => setFiltros({ status: '', tipo: '', cursoId: '' })}>Limpar filtros</button>
      </div>

      {carregando
        ? <div className="panel"><div className="skeleton" style={{ height: 180 }} /></div>
        : (
          <DataTable colunas={colunas} dados={salas} porPagina={15}
            buscaPlaceholder="Buscar por nome ou código…"
            classeLinha={(s) => (s.status === 'INATIVO' ? 'dim' : '')}
            vazio={<EmptyState icone="🏛" titulo="Nenhum ambiente encontrado"
              descricao="Ajuste os filtros ou cadastre um novo ambiente." />} />
        )}

      {form && (
        <Drawer titulo={form.id ? 'Editar ambiente' : 'Novo ambiente'} onClose={() => setForm(null)}
          subtitulo={form.id ? form.nome : 'Cadastre a sala ou laboratório e defina a visibilidade'}
          rodape={(
            <>
              <button className="btn btn-secondary" onClick={() => setForm(null)}>Cancelar</button>
              <button className="btn" onClick={salvar} disabled={ocupado}>
                {ocupado ? 'Salvando…' : 'Salvar'}
              </button>
            </>
          )}>
          <div className="split">
            <Field label="Nome">
              <input className="input" value={form.nome} placeholder="Ex: Sala 1001"
                onChange={(e) => setForm({ ...form, nome: e.target.value })} />
            </Field>
            <Field label="Código">
              <input className="input" value={form.codigo} placeholder="Ex: 1001"
                onChange={(e) => setForm({ ...form, codigo: e.target.value })} />
            </Field>
          </div>

          <Field label="Tipo de ambiente">
            <select className="select" value={form.tipo}
              onChange={(e) => setForm({ ...form, tipo: e.target.value })}>
              {tipos.map((t) => <option key={t.valor} value={t.valor}>{t.rotulo}</option>)}
            </select>
          </Field>

          <div className="split">
            <Field label="Capacidade">
              <input className="input" type="number" min="0" value={form.capacidade}
                onChange={(e) => setForm({ ...form, capacidade: e.target.value })} />
            </Field>
            <Field label="Andar / bloco">
              <input className="input" value={form.andar} placeholder="Ex: 1º andar"
                onChange={(e) => setForm({ ...form, andar: e.target.value })} />
            </Field>
          </div>

          <Field label="Cursos com acesso"
            hint="Visibilidade setorizada: apenas professores destes cursos veem e reservam o ambiente.">
            <div className="check-list">
              {cursos.map((c) => (
                <label className="check" key={c.id}>
                  <input type="checkbox" checked={form.cursoIds.includes(c.id)}
                    onChange={() => setForm((f) => ({
                      ...f,
                      cursoIds: f.cursoIds.includes(c.id)
                        ? f.cursoIds.filter((x) => x !== c.id)
                        : [...f.cursoIds, c.id],
                    }))} />
                  <span>{c.nome}{c.sigla ? ` (${c.sigla})` : ''}</span>
                </label>
              ))}
            </div>
          </Field>
        </Drawer>
      )}

      {acao?.tipo === 'reativar' && (
        <ConfirmDialog
          titulo="Reativar ambiente"
          tom="ok"
          confirmarTexto="Reativar"
          mensagem={`${acao.sala.nome} volta a aceitar reservas dos cursos vinculados.`}
          ocupado={ocupado}
          onConfirm={reativar}
          onClose={() => setAcao(null)} />
      )}

      {excluindo && (
        <DialogoExclusao recurso="salas" rotulo="ambiente" item={excluindo}
          onFechar={() => setExcluindo(null)}
          onConcluido={(msg) => { toast.sucesso(msg); setExcluindo(null); carregar(); }} />
      )}
    </>
  );
}
