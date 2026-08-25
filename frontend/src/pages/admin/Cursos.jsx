import { useCallback, useEffect, useState } from 'react';
import { api, qs } from '../../api/client';
import DataTable from '../../components/ui/DataTable';
import { useToast } from '../../components/ui/ToastProvider';
import {
  ConfirmDialog, Drawer, EmptyState, Field, Notice, PageHeader, StatusBadge,
} from '../../components/ui/primitives';
import DialogoExclusao from '../../components/admin/DialogoExclusao';

const VAZIO = { nome: '', sigla: '' };

/** CRUD de cursos com o mesmo ciclo de vida dos ambientes. */
export default function Cursos() {
  const toast = useToast();
  const [cursos, setCursos] = useState([]);
  const [filtros, setFiltros] = useState({ status: '' });
  const [form, setForm] = useState(null);
  const [acao, setAcao] = useState(null);      // apenas reativacao
  const [excluindo, setExcluindo] = useState(null);
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(true);
  const [ocupado, setOcupado] = useState(false);

  const carregar = useCallback(() => {
    setCarregando(true);
    api.get(`/cursos${qs(filtros)}`)
      .then(setCursos)
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }, [filtros]);

  useEffect(() => { carregar(); }, [carregar]);

  async function salvar() {
    setOcupado(true);
    try {
      if (form.id) await api.put(`/cursos/${form.id}`, { nome: form.nome, sigla: form.sigla });
      else await api.post('/cursos', { nome: form.nome, sigla: form.sigla });
      toast.sucesso(form.id ? 'Curso atualizado.' : 'Curso cadastrado.');
      setForm(null);
      carregar();
    } catch (e) { toast.erro(e.message); } finally { setOcupado(false); }
  }

  async function reativar() {
    setOcupado(true);
    try {
      await api.post(`/cursos/${acao.curso.id}/reativar`);
      toast.sucesso('Curso reativado.');
      setAcao(null);
      carregar();
    } catch (e) { toast.erro(e.message); } finally { setOcupado(false); }
  }

  const colunas = [
    { chave: 'nome', titulo: 'Curso', render: (c) => <strong>{c.nome}</strong> },
    { chave: 'sigla', titulo: 'Sigla',
      render: (c) => (c.sigla ? <span className="tag plain">{c.sigla}</span> : <span className="text-muted">—</span>) },
    { chave: 'status', titulo: 'Status', render: (c) => <StatusBadge valor={c.status} /> },
    { chave: 'acoes', titulo: '', ordenavel: false, alinhar: 'right',
      render: (c) => (
        <>
          <button className="btn btn-secondary btn-sm"
            onClick={() => setForm({ id: c.id, nome: c.nome, sigla: c.sigla || '' })}>Editar</button>
          {c.status === 'INATIVO' && (
            <button className="btn btn-secondary btn-sm"
              onClick={() => setAcao({ curso: c, tipo: 'reativar' })}>Reativar</button>
          )}
          <button className="btn btn-danger btn-sm" onClick={() => setExcluindo(c)}>Excluir</button>
        </>
      ) },
  ];

  return (
    <>
      <PageHeader titulo="Cursos"
        descricao="Um curso inativo deixa de conceder visibilidade sobre os ambientes vinculados."
        acoes={<button className="btn" onClick={() => setForm({ ...VAZIO })}>+ Novo curso</button>} />

      {erro && <div className="mb-4"><Notice tom="danger">{erro}</Notice></div>}

      <div className="filterbar">
        <Field label="Status">
          <select className="select" value={filtros.status}
            onChange={(e) => setFiltros({ status: e.target.value })}>
            <option value="">Todos</option>
            <option value="ATIVO">Ativos</option>
            <option value="INATIVO">Inativos</option>
          </select>
        </Field>
        <div className="spacer" />
        <button className="btn btn-secondary" onClick={() => setFiltros({ status: '' })}>Limpar filtros</button>
      </div>

      {carregando
        ? <div className="panel"><div className="skeleton" style={{ height: 160 }} /></div>
        : (
          <DataTable colunas={colunas} dados={cursos} buscaPlaceholder="Buscar por nome ou sigla…"
            classeLinha={(c) => (c.status === 'INATIVO' ? 'dim' : '')}
            vazio={<EmptyState icone="🎓" titulo="Nenhum curso encontrado"
              descricao="Cadastre um curso para vincular usuários e ambientes." />} />
        )}

      {form && (
        <Drawer titulo={form.id ? 'Editar curso' : 'Novo curso'} onClose={() => setForm(null)}
          rodape={(
            <>
              <button className="btn btn-secondary" onClick={() => setForm(null)}>Cancelar</button>
              <button className="btn" onClick={salvar} disabled={ocupado}>
                {ocupado ? 'Salvando…' : 'Salvar'}
              </button>
            </>
          )}>
          <Field label="Nome do curso">
            <input className="input" value={form.nome} placeholder="Ex: Ciência da Computação"
              onChange={(e) => setForm({ ...form, nome: e.target.value })} />
          </Field>
          <Field label="Sigla" hint="Usada nas etiquetas das tabelas e da agenda.">
            <input className="input" maxLength={20} value={form.sigla} placeholder="Ex: CC"
              onChange={(e) => setForm({ ...form, sigla: e.target.value })} />
          </Field>
        </Drawer>
      )}

      {acao?.tipo === 'reativar' && (
        <ConfirmDialog
          titulo="Reativar curso"
          tom="ok"
          confirmarTexto="Reativar"
          mensagem={`${acao.curso.nome} volta a conceder acesso aos ambientes vinculados.`}
          ocupado={ocupado}
          onConfirm={reativar}
          onClose={() => setAcao(null)} />
      )}

      {excluindo && (
        <DialogoExclusao recurso="cursos" rotulo="curso" item={excluindo}
          onFechar={() => setExcluindo(null)}
          onConcluido={(msg) => { toast.sucesso(msg); setExcluindo(null); carregar(); }} />
      )}
    </>
  );
}
