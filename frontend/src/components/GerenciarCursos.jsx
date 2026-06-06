import { useEffect, useState } from 'react';
import { api } from '../api/client';

export default function GerenciarCursos() {
  const [cursos, setCursos] = useState([]);
  const [nome, setNome] = useState('');
  const [editId, setEditId] = useState(null);
  const [erro, setErro] = useState('');
  const [showForm, setShowForm] = useState(false);

  async function carregar() {
    try {
      setCursos(await api.get('/cursos'));
    } catch (e) {
      setErro(e.message);
    }
  }

  useEffect(() => { carregar(); }, []);

  function abrirNovo() {
    setEditId(null);
    setNome('');
    setShowForm(true);
    setErro('');
  }

  function abrirEdicao(c) {
    setEditId(c.id);
    setNome(c.nome);
    setShowForm(true);
    setErro('');
  }

  async function salvar() {
    setErro('');
    try {
      if (editId) {
        await api.put(`/cursos/${editId}`, { nome });
      } else {
        await api.post('/cursos', { nome });
      }
      setShowForm(false);
      carregar();
    } catch (e) {
      setErro(e.message);
    }
  }

  async function desativar(id) {
    if (!confirm('Desativar este curso?')) return;
    try {
      await api.del(`/cursos/${id}`);
      carregar();
    } catch (e) {
      setErro(e.message);
    }
  }

  return (
    <div>
      <p className="muted" style={{ marginBottom: 12 }}>Cursos Cadastrados</p>
      <button className="btn btn-sm btn-green" onClick={abrirNovo}>+ Adicionar Curso</button>

      {showForm && (
        <div className="card" style={{ marginTop: 16 }}>
          <label>Nome do Curso</label>
          <input value={nome} onChange={(e) => setNome(e.target.value)} placeholder="Ex: Ciencia da Computacao" />
          <div style={{ display: 'flex', gap: 8, marginTop: 14 }}>
            <button className="btn btn-sm" onClick={salvar}>{editId ? 'Atualizar' : 'Salvar'}</button>
            <button className="btn btn-sm btn-danger" onClick={() => setShowForm(false)}>Cancelar</button>
          </div>
          {erro && <p className="error">{erro}</p>}
        </div>
      )}

      <div style={{ marginTop: 18 }}>
        {cursos.map((c) => (
          <div className="card" key={c.id}>
            <div className="card-row">
              <div>
                <strong>{c.nome}</strong>
                <p className="muted">ID #{c.id}</p>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-sm" onClick={() => abrirEdicao(c)}>Editar</button>
                <button className="btn btn-sm btn-danger" onClick={() => desativar(c.id)}>Desativar</button>
              </div>
            </div>
          </div>
        ))}
        {cursos.length === 0 && <p className="muted">Nenhum curso cadastrado.</p>}
      </div>
    </div>
  );
}
