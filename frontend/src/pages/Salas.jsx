import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function Salas() {
  const { user } = useAuth();
  const [salas, setSalas] = useState([]);
  const [erro, setErro] = useState('');

  useEffect(() => {
    (async () => {
      try {
        // Admin/gestor (sem curso) ve todas; solicitante ve as do seu curso
        const q = user?.cursoId ? `/salas?cursoId=${user.cursoId}` : '/salas';
        setSalas(await api.get(q));
      } catch (e) { setErro(e.message); }
    })();
  }, []);

  return (
    <div className="page">
      <h1>Todas as Salas</h1>
      <p className="lead">Ambientes disponíveis para o seu curso</p>
      {erro && <p className="error">{erro}</p>}
      <div className="grid">
        {salas.map((s) => (
          <div className="card" key={s.id}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <span className="ic" style={{ width: 38, height: 38, borderRadius: 8, background: 'var(--cf-green-light)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>🏫</span>
              <div>
                <strong>{s.nome}</strong>
                <p className="muted">{s.andar}</p>
              </div>
            </div>
            <p className="muted" style={{ marginTop: 10 }}>👥 {s.capacidade} lugares · {s.tipo}</p>
            <div style={{ marginTop: 8 }}>
              {s.cursos?.map((c) => <span className="tag" key={c.id}>{c.nome}</span>)}
            </div>
          </div>
        ))}
        {salas.length === 0 && <p className="muted">Nenhuma sala visível para seu curso.</p>}
      </div>
    </div>
  );
}
