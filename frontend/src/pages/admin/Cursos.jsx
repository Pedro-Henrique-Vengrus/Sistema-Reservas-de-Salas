import { useCallback, useEffect, useState } from 'react';
import { api, qs } from '../../api/client';
import DataTable from '../../components/ui/DataTable';
import { useToast } from '../../components/ui/ToastProvider';
import {
  ConfirmDialog, Drawer, EmptyState, Field, Notice, PageHeader, StatusBadge,
} from '../../components/ui/primitives';

const VAZIO = { nome: '', sigla: '' };

/** CRUD de cursos com o mesmo ciclo de vida dos ambientes. */
export default function Cursos() {
  const toast = useToast();
  const [cursos, setCursos] = useState([]);
  const [filtros, setFiltros] = useState({ status: '' });
  const [form, setForm] = useState(null);
  const [acao, setAcao] = useState(null);
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

  async function pedirInativacao(curso) {
    try {
      await api.del(`/cursos/${curso.id}`);
      toast.sucesso('Curso inativado.');
      carregar();
    } catch (e) {
      if (e.status === 409 && e.detalhes) setAcao({ curso, tipo: 'inativar', detalhes: e.detalhes, mensagem: e.message });
      else toast.erro(e.message);
    }
  }

  async function confirmarAcao() {
    setOcupado(true);
    try {
      if (acao.tipo === 'inativar') {
        await api.del(`/cursos/${acao.curso.id}?forcar=true`);
        toast.sucesso('Curso inativado e reservas futuras canceladas.');
      } else if (acao.tipo === 'reativar') {
        await api.post(`/cursos/${acao.curso.id}/reativar`);
        toast.sucesso('Curso reativado.');
      } else {
        await api.del(`/cursos/${acao.curso.id}/permanente`);
        toast.sucesso('Curso excluído definitivamente.');
      }
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
          {c.status === 'ATIVO'
            ? <button className="btn btn-danger btn-sm" onClick={() => pedirInativacao(c)}>Inativar</button>
            : (
              <>
                <button className="btn btn-secondary btn-sm"
                  onClick={() => setAcao({ curso: c, tipo: 'reativar' })}>Reativar</button>
                <button className="btn btn-danger btn-sm"
                  onClick={() => setAcao({ curso: c, tipo: 'excluir' })}>Excluir</button>
              </>
            )}
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

      {acao && (
        <ConfirmDialog
          titulo={{
            inativar: 'Inativar curso com reservas ativas',
            reativar: 'Reativar curso',
            excluir: 'Excluir definitivamente',
          }[acao.tipo]}
          tom={acao.tipo === 'reativar' ? 'ok' : 'danger'}
          confirmarTexto={{
            inativar: 'Inativar e cancelar reservas',
            reativar: 'Reativar',
            excluir: 'Excluir definitivamente',
          }[acao.tipo]}
          mensagem={acao.mensagem || {
            reativar: `${acao.curso.nome} volta a conceder acesso aos ambientes vinculados.`,
            excluir: `${acao.curso.nome} será removido do banco. Só é permitido quando não há usuários vinculados nem reservas futuras.`,
          }[acao.tipo]}
          detalhes={acao.detalhes}
          ocupado={ocupado}
          onConfirm={confirmarAcao}
          onClose={() => setAcao(null)} />
      )}
    </>
  );
}
