import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function Salas() {
  const { user, isAdmin } = useAuth();
  const [salas, setSalas] = useState([]);
  const [erro, setErro] = useState('');

  const semCurso = !isAdmin && !user?.cursoId;

  useEffect(() => {
    (async () => {
      try {
        // Quem ve todas e decidido pela ROLE (GESTOR), nao pela ausencia de curso.
        // Solicitante ve so as do seu curso; sem curso vinculado, nao ve nenhuma.
        if (isAdmin) {
          setSalas(await api.get('/salas'));
        } else if (user?.cursoId) {
          setSalas(await api.get(`/salas?cursoId=${user.cursoId}`));
        } else {
          setSalas([]);
        }
      } catch (e) { setErro(e.message); }
    })();
  }, []);

  return (
    <div className="page">
      <h1>Todas as Salas</h1>
      <p className="lead">Ambientes disponíveis para o seu curso</p>
      {erro && <p className="error">{erro}</p>}
      {semCurso && <p className="error">Você não está vinculado a nenhum curso. Contate o gestor.</p>}
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
        {!semCurso && salas.length === 0 && <p className="muted">Nenhuma sala visível para seu curso.</p>}
      </div>
    </div>
  );
}
