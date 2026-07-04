import { useEffect, useState } from 'react';
import { api } from '../api/client';

function pillStatus(status) {
  const map = { PENDENTE: 'pill-pendente', CONFIRMADA: 'pill-aceita', RECUSADA: 'pill-recusada', CANCELADA: 'pill-recusada' };
  const label = { PENDENTE: 'Pendente', CONFIRMADA: 'Confirmada', RECUSADA: 'Recusada', CANCELADA: 'Cancelada' };
  return <span className={`pill ${map[status] || ''}`}>{label[status] || status}</span>;
}

export default function TodasReservas() {
  const [reservas, setReservas] = useState([]);
  const [erro, setErro] = useState('');
  const [toast, setToast] = useState('');

  function showToast(m) { setToast(m); setTimeout(() => setToast(''), 3000); }

  async function carregar() {
    try {
      setReservas(await api.get('/reservas/todas'));
    } catch (e) { setErro(e.message); }
  }

  useEffect(() => { carregar(); }, []);

  async function cancelar(id) {
    if (!confirm('Cancelar esta reserva?')) return;
    try {
      await api.del(`/reservas/${id}`);
      showToast('Reserva cancelada');
      carregar();
    } catch (e) { setErro(e.message); }
  }

  return (
    <div>
      {toast && <div className="toast">{toast}</div>}
      <p className="muted" style={{ marginBottom: 12 }}>Todas as Reservas do Sistema</p>
      {erro && <p className="error">{erro}</p>}

      <div style={{ marginTop: 8 }}>
        {reservas.map((r) => (
          <div className="card" key={r.id}>
            <div className="card-row">
              <div>
                <strong>{r.salaNome}</strong> <span className="muted">· {r.salaAndar}</span>
                <p className="muted">Reservado por: {r.solicitanteNome}</p>
                <p className="muted">{r.data} • {r.horaInicio.slice(0, 5)} às {r.horaFim.slice(0, 5)}</p>
                <div style={{ marginTop: 6 }}>{pillStatus(r.status)}</div>
              </div>
              {(r.status === 'PENDENTE' || r.status === 'CONFIRMADA') && (
                <button className="btn btn-sm btn-danger" onClick={() => cancelar(r.id)}>Cancelar</button>
              )}
            </div>
          </div>
        ))}
        {reservas.length === 0 && <p className="muted">Nenhuma reserva no sistema.</p>}
      </div>
    </div>
  );
}
