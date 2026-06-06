import { useEffect, useState } from 'react';
import { api } from '../api/client';

const emptyForm = { nome: '', tipo: '', capacidade: 0, andar: '', cursoIds: [] };

export default function GerenciarSalas() {
  const [salas, setSalas] = useState([]);
  const [cursos, setCursos] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editId, setEditId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [erro, setErro] = useState('');

  async function carregar() {
    try {
      const [s, c] = await Promise.all([api.get('/salas'), api.get('/cursos')]);
      setSalas(s);
      setCursos(c);
    } catch (e) {
      setErro(e.message);
    }
  }

  useEffect(() => { carregar(); }, []);

  function abrirNovo() {
    setEditId(null);
    setForm(emptyForm);
    setShowForm(true);
    setErro('');
  }

  function abrirEdicao(s) {
    setEditId(s.id);
    setForm({
      nome: s.nome,
      tipo: s.tipo || '',
      capacidade: s.capacidade,
      andar: s.andar || '',
      cursoIds: s.cursos.map((c) => c.id),
    });
    setShowForm(true);
    setErro('');
  }

  function toggleCurso(id) {
    setForm((f) => ({
      ...f,
      cursoIds: f.cursoIds.includes(id)
        ? f.cursoIds.filter((x) => x !== id)
        : [...f.cursoIds, id],
    }));
  }

  async function salvar() {
    setErro('');
    try {
      const payload = { ...form, capacidade: Number(form.capacidade) };
      if (editId) {
        await api.put(`/salas/${editId}`, payload);
      } else {
        await api.post('/salas', payload);
      }
      setShowForm(false);
      carregar();
    } catch (e) {
      setErro(e.message);
    }
  }

  async function desativar(id) {
    if (!confirm('Desativar esta sala?')) return;
    try {
      await api.del(`/salas/${id}`);
      carregar();
    } catch (e) {
      setErro(e.message);
    }
  }

  return (
    <div>
      <p className="muted" style={{ marginBottom: 12 }}>Salas Cadastradas</p>
      <button className="btn btn-sm btn-green" onClick={abrirNovo}>+ Adicionar Sala</button>

      {showForm && (
        <div className="card" style={{ marginTop: 16 }}>
          <label>Nome</label>
          <input value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} placeholder="Ex: Sala 1001" />

          <label>Tipo</label>
          <input value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })} placeholder="Ex: Laboratorio" />

          <label>Capacidade</label>
          <input type="number" value={form.capacidade} onChange={(e) => setForm({ ...form, capacidade: e.target.value })} />

          <label>Andar</label>
          <input value={form.andar} onChange={(e) => setForm({ ...form, andar: e.target.value })} placeholder="Ex: 1 Andar" />

          <label>Cursos com acesso (visibilidade setorizada)</label>
          {cursos.map((c) => (
            <div className="checkbox-row" key={c.id}>
              <input
                type="checkbox"
                checked={form.cursoIds.includes(c.id)}
                onChange={() => toggleCurso(c.id)}
              />
              <span>{c.nome}</span>
            </div>
          ))}

          <div style={{ display: 'flex', gap: 8, marginTop: 14 }}>
            <button className="btn btn-sm" onClick={salvar}>{editId ? 'Atualizar' : 'Salvar'}</button>
            <button className="btn btn-sm btn-danger" onClick={() => setShowForm(false)}>Cancelar</button>
          </div>
          {erro && <p className="error">{erro}</p>}
        </div>
      )}

      <div style={{ marginTop: 18 }}>
        {salas.map((s) => (
          <div className="card" key={s.id}>
            <div className="card-row">
              <div>
                <strong>{s.nome}</strong> <span className="muted">· {s.andar}</span>
                <p className="muted">{s.tipo} · {s.capacidade} lugares · {s.situacao}</p>
                <div style={{ marginTop: 6 }}>
                  {s.cursos.map((c) => <span className="tag" key={c.id}>{c.nome}</span>)}
                </div>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-sm" onClick={() => abrirEdicao(s)}>Editar</button>
                <button className="btn btn-sm btn-danger" onClick={() => desativar(s.id)}>Desativar</button>
              </div>
            </div>
          </div>
        ))}
        {salas.length === 0 && <p className="muted">Nenhuma sala cadastrada.</p>}
      </div>
    </div>
  );
}
