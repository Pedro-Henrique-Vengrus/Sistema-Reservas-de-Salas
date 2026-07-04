import { useEffect, useState } from 'react';
import { api } from '../api/client';

export default function AprovarReservas() {
  const [pendentes, setPendentes] = useState([]);
  const [erro, setErro] = useState('');
  const [toast, setToast] = useState('');

  function showToast(m) { setToast(m); setTimeout(() => setToast(''), 3000); }

  async function carregar() {
    try {
      setPendentes(await api.get('/reservas/pendentes'));
    } catch (e) { setErro(e.message); }
  }

  useEffect(() => { carregar(); }, []);

  async function avaliar(id, aprovar) {
    try {
      await api.post(`/reservas/${id}/${aprovar ? 'aprovar' : 'recusar'}`);
      showToast(aprovar ? '✓ Reserva aprovada!' : 'Reserva recusada');
      carregar();
    } catch (e) { setErro(e.message); }
  }

  return (
    <div>
      {toast && <div className="toast">{toast}</div>}
      <p className="muted" style={{ marginBottom: 12 }}>Reservas Aguardando Aprovação</p>
      {erro && <p className="error">{erro}</p>}

      <div style={{ marginTop: 8 }}>
        {pendentes.map((r) => (
          <div className="card" key={r.id}>
            <div className="card-row">
              <div>
                <strong>{r.salaNome}</strong> <span className="muted">· {r.salaAndar}</span>
                <p className="muted">Solicitado por: {r.solicitanteNome}</p>
                <p className="muted">{r.data} • {r.horaInicio.slice(0, 5)} às {r.horaFim.slice(0, 5)}</p>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-sm" onClick={() => avaliar(r.id, true)}>✓ Aprovar</button>
                <button className="btn btn-sm btn-danger" onClick={() => avaliar(r.id, false)}>✗ Recusar</button>
              </div>
            </div>
          </div>
        ))}
        {pendentes.length === 0 && <p className="muted">Nenhuma reserva pendente de aprovação.</p>}
      </div>
    </div>
  );
}
