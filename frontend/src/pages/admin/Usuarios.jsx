import { useCallback, useEffect, useState } from 'react';
import { api, qs } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/ui/DataTable';
import { useToast } from '../../components/ui/ToastProvider';
import {
  ConfirmDialog, Drawer, EmptyState, Field, Notice, PageHeader, StatusBadge,
} from '../../components/ui/primitives';

const PERFIS = [
  { valor: 'PROFESSOR', rotulo: 'Professor' },
  { valor: 'REITOR', rotulo: 'Reitor' },
  { valor: 'ADMIN', rotulo: 'Admin' },
];

const VAZIO = { nome: '', email: '', senha: '', role: 'PROFESSOR', cursoIds: [] };

/** CRUD de usuarios com exclusao logica e atribuicao de cursos. */
export default function Usuarios() {
  const toast = useToast();
  const { user } = useAuth();
  const [usuarios, setUsuarios] = useState([]);
  const [cursos, setCursos] = useState([]);
  const [filtros, setFiltros] = useState({ role: '', status: '', cursoId: '' });
  const [form, setForm] = useState(null);   // { ...campos, id? }
  const [confirmar, setConfirmar] = useState(null);
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(true);
  const [ocupado, setOcupado] = useState(false);

  const carregar = useCallback(() => {
    setCarregando(true);
    Promise.all([
      api.get(`/usuarios${qs(filtros)}`),
      api.get('/cursos?status=ATIVO'),
    ])
      .then(([u, c]) => { setUsuarios(u); setCursos(c); })
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }, [filtros]);

  useEffect(() => { carregar(); }, [carregar]);

  async function salvar() {
    setOcupado(true);
    try {
      const payload = {
        nome: form.nome,
        email: form.email,
        senha: form.senha || null,
        role: form.role,
        cursoIds: form.cursoIds,
      };
      if (form.id) await api.put(`/usuarios/${form.id}`, payload);
      else await api.post('/usuarios', payload);
      toast.sucesso(form.id ? 'Usuário atualizado.' : 'Usuário criado.');
      setForm(null);
      carregar();
    } catch (e) { toast.erro(e.message); } finally { setOcupado(false); }
  }

  async function alternarStatus(u) {
    setOcupado(true);
    try {
      if (u.status === 'ATIVO') await api.del(`/usuarios/${u.id}`);
      else await api.post(`/usuarios/${u.id}/reativar`);
      toast.sucesso(u.status === 'ATIVO' ? 'Usuário desativado.' : 'Usuário reativado.');
      setConfirmar(null);
      carregar();
    } catch (e) { toast.erro(e.message); } finally { setOcupado(false); }
  }

  const colunas = [
    { chave: 'nome', titulo: 'Usuário',
      render: (u) => (
        <div>
          <strong>{u.nome}</strong>
          <div className="text-sm text-muted">{u.email}</div>
        </div>
      ) },
    { chave: 'role', titulo: 'Perfil', render: (u) => <StatusBadge valor={u.role} /> },
    { chave: 'cursos', titulo: 'Cursos vinculados', ordenavel: false,
      valor: (u) => u.cursos.map((c) => c.nome).join(' '),
      render: (u) => (u.cursos.length
        ? u.cursos.map((c) => <span className="tag" key={c.id}>{c.sigla || c.nome}</span>)
        : <span className="text-muted text-sm">—</span>) },
    { chave: 'status', titulo: 'Status', render: (u) => <StatusBadge valor={u.status} /> },
    { chave: 'acoes', titulo: '', ordenavel: false, alinhar: 'right',
      render: (u) => (
        <>
          <button className="btn btn-secondary btn-sm"
            onClick={() => setForm({
              id: u.id, nome: u.nome, email: u.email, senha: '',
              role: u.role, cursoIds: u.cursos.map((c) => c.id),
            })}>
            Editar
          </button>
          {u.id !== user?.id && (
            <button className={`btn btn-sm ${u.status === 'ATIVO' ? 'btn-danger' : 'btn-secondary'}`}
              onClick={() => setConfirmar(u)}>
              {u.status === 'ATIVO' ? 'Desativar' : 'Reativar'}
            </button>
          )}
        </>
      ) },
  ];

  return (
    <>
      <PageHeader titulo="Usuários"
        descricao="Cadastro, perfis de acesso e vínculo com cursos — a base da visibilidade setorizada."
        acoes={<button className="btn" onClick={() => setForm({ ...VAZIO })}>+ Novo usuário</button>} />

      {erro && <div className="mb-4"><Notice tom="danger">{erro}</Notice></div>}

      <div className="filterbar">
        <Field label="Perfil">
          <select className="select" value={filtros.role}
            onChange={(e) => setFiltros({ ...filtros, role: e.target.value })}>
            <option value="">Todos</option>
            {PERFIS.map((p) => <option key={p.valor} value={p.valor}>{p.rotulo}</option>)}
          </select>
        </Field>
        <Field label="Status">
          <select className="select" value={filtros.status}
            onChange={(e) => setFiltros({ ...filtros, status: e.target.value })}>
            <option value="">Todos</option>
            <option value="ATIVO">Ativos</option>
            <option value="INATIVO">Inativos</option>
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
          onClick={() => setFiltros({ role: '', status: '', cursoId: '' })}>Limpar filtros</button>
      </div>

      {carregando
        ? <div className="panel"><div className="skeleton" style={{ height: 180 }} /></div>
        : (
          <DataTable colunas={colunas} dados={usuarios} porPagina={15}
            buscaPlaceholder="Buscar por nome ou e-mail…"
            classeLinha={(u) => (u.status === 'INATIVO' ? 'dim' : '')}
            vazio={<EmptyState icone="👤" titulo="Nenhum usuário encontrado"
              descricao="Ajuste os filtros ou cadastre um novo usuário." />} />
        )}

      {form && (
        <Drawer titulo={form.id ? 'Editar usuário' : 'Novo usuário'}
          subtitulo={form.id ? form.email : 'Defina o perfil e os cursos de atuação'}
          onClose={() => setForm(null)}
          rodape={(
            <>
              <button className="btn btn-secondary" onClick={() => setForm(null)}>Cancelar</button>
              <button className="btn" onClick={salvar} disabled={ocupado}>
                {ocupado ? 'Salvando…' : 'Salvar'}
              </button>
            </>
          )}>
          <Field label="Nome completo">
            <input className="input" value={form.nome} placeholder="Ex: Pedro Henrique"
              onChange={(e) => setForm({ ...form, nome: e.target.value })} />
          </Field>

          <Field label="E-mail institucional">
            <input className="input" type="email" value={form.email} placeholder="pedro@campus.br"
              onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </Field>

          <Field label={form.id ? 'Nova senha' : 'Senha inicial'}
            hint={form.id ? 'Deixe em branco para manter a senha atual.' : 'O usuário poderá alterá-la depois.'}>
            <input className="input" type="password" value={form.senha} autoComplete="new-password"
              onChange={(e) => setForm({ ...form, senha: e.target.value })} />
          </Field>

          <Field label="Perfil de acesso"
            hint="Somente o Admin opera o painel. Reitor e Professor solicitam reservas e enxergam apenas os ambientes dos seus cursos.">
            <select className="select" value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value })}>
              {PERFIS.map((p) => <option key={p.valor} value={p.valor}>{p.rotulo}</option>)}
            </select>
          </Field>

          <Field label="Cursos vinculados"
            hint={form.role === 'ADMIN'
              ? 'O perfil Admin não solicita reservas, então o vínculo é opcional.'
              : 'Obrigatório: define quais ambientes o usuário enxerga e pode reservar.'}>
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

      {confirmar && (
        <ConfirmDialog
          titulo={confirmar.status === 'ATIVO' ? 'Desativar usuário' : 'Reativar usuário'}
          tom={confirmar.status === 'ATIVO' ? 'danger' : 'ok'}
          confirmarTexto={confirmar.status === 'ATIVO' ? 'Desativar' : 'Reativar'}
          mensagem={confirmar.status === 'ATIVO'
            ? `${confirmar.nome} deixará de acessar o sistema. O histórico de reservas é preservado (exclusão lógica).`
            : `${confirmar.nome} voltará a acessar o sistema com os mesmos vínculos.`}
          ocupado={ocupado}
          onConfirm={() => alternarStatus(confirmar)}
          onClose={() => setConfirmar(null)} />
      )}
    </>
  );
}
