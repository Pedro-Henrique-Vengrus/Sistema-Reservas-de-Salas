import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';

function pillStatus(status) {
  const map = { PENDENTE: 'pill-pendente', CONFIRMADA: 'pill-aceita', RECUSADA: 'pill-recusada' };
  const label = { PENDENTE: 'Pendente', CONFIRMADA: 'Confirmada', RECUSADA: 'Recusada' };
  return <span className={`pill ${map[status] || ''}`}>{label[status] || status}</span>;
}

export default function MinhasReservas() {
  const { isAdmin } = useAuth();
  const [minhas, setMinhas] = useState([]);
  const [outros, setOutros] = useState([]);
  const [toast, setToast] = useState('');
  const [erro, setErro] = useState('');
  const [modal, setModal] = useState(null);

  function showToast(m) { setToast(m); setTimeout(() => setToast(''), 3000); }

  async function carregar() {
    try {
      if (isAdmin) {
        setMinhas(await api.get('/reservas/minhas'));
        return;
      }
      const [m, o] = await Promise.all([
        api.get('/reservas/minhas'),
        api.get('/reservas/outros'),
      ]);
      setMinhas(m);
      setOutros(o);
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

  function passada(r) {
    const dt = new Date(`${r.data}T${r.horaFim}`);
    return dt < new Date();
  }

  return (
    <div className="page">
      {toast && <div className="toast">{toast}</div>}
      <h1>Minhas Salas Reservadas</h1>
      {erro && <p className="error">{erro}</p>}

      <div className="section-title">Minhas Reservas</div>
      {minhas.map((r) => (
        <div className="list-item" key={r.id} style={passada(r) ? { opacity: 0.55 } : {}}>
          <div className="list-item-left">
            <span className="ic">🖥</span>
            <div>
              <strong>{r.salaNome}</strong>
              <p className="muted">
                {r.data} • {r.horaInicio.slice(0,5)} {' '}
                {passada(r) ? <span className="pill pill-recusada" style={{ marginLeft: 6 }}>Passada</span> : pillStatus(r.status)}
              </p>
            </div>
          </div>
          {!passada(r) && r.status !== 'RECUSADA' && <button className="btn btn-danger btn-sm" onClick={() => cancelar(r.id)}>Cancelar</button>}
        </div>
      ))}
      {minhas.length === 0 && <p className="muted">Você ainda não tem reservas.</p>}

      {!isAdmin && (
        <>
          <div className="section-title">Reservas de Outros (Propor Troca)</div>
          {outros.map((r) => (
            <div className="list-item" key={r.id} style={{ background: '#f0f7ff' }}>
              <div className="list-item-left">
                <span className="ic">🖥</span>
                <div>
                  <strong>{r.salaNome}</strong>
                  <p className="muted">{r.solicitanteNome} · {r.data} • {r.horaInicio.slice(0,5)}</p>
                </div>
              </div>
              <button className="btn btn-blue btn-sm" onClick={() => setModal(r)}>⇄ Propor Troca</button>
            </div>
          ))}
          {outros.length === 0 && <p className="muted">Nenhuma reserva de outros usuários.</p>}
        </>
      )}

      {modal && (
        <ModalTroca reserva={modal} minhasReservas={minhas} onClose={() => setModal(null)}
          onSucesso={() => { setModal(null); showToast('✓ Proposta enviada!'); carregar(); }} />
      )}
    </div>
  );
}

function ModalTroca({ reserva, minhasReservas, onClose, onSucesso }) {
  const [justificativa, setJustificativa] = useState('');
  const [reservaOferecidaId, setReservaOferecidaId] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  const elegiveis = minhasReservas.filter((r) => {
    if (r.status !== 'CONFIRMADA') return false;
    const fim = new Date(`${r.data}T${r.horaFim}`);
    return fim >= new Date();
  });

  async function enviar() {
    if (!reservaOferecidaId) { setErro('Selecione qual das suas salas você quer oferecer.'); return; }
    if (!justificativa.trim()) { setErro('A justificativa é obrigatória.'); return; }
    setEnviando(true); setErro('');
    try {
      await api.post('/propostas', { reservaOrigemId: reserva.id, reservaOferecidaId: Number(reservaOferecidaId), justificativa });
      onSucesso();
    } catch (e) { setErro(e.message); setEnviando(false); }
  }

  return (
    <div className="modal-bg" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>⇄ Propor Troca de Sala</h3>
        <div className="info-box">
          <strong>{reserva.salaNome}</strong><br />
          {reserva.data} às {reserva.horaInicio.slice(0,5)}<br />
          Reservado por: <strong>{reserva.solicitanteNome}</strong>
        </div>

        {elegiveis.length === 0 ? (
          <p className="error">Você precisa de uma reserva confirmada para propor troca.</p>
        ) : (
          <>
            <label>Sua sala oferecida em troca</label>
            <select value={reservaOferecidaId} onChange={(e) => setReservaOferecidaId(e.target.value)}>
              <option value="">Selecione...</option>
              {elegiveis.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.salaNome} · {r.data} às {r.horaInicio.slice(0, 5)}
                </option>
              ))}
            </select>

            <label>Justificativa (obrigatória)</label>
            <textarea value={justificativa} onChange={(e) => setJustificativa(e.target.value)}
              placeholder="Explique o motivo da troca..." />
          </>
        )}

        {erro && <p className="error">{erro}</p>}
        <div className="modal-actions">
          <button className="btn btn-blue" onClick={enviar} disabled={enviando || elegiveis.length === 0}>Enviar Proposta</button>
          <button className="btn btn-outline" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  );
}
