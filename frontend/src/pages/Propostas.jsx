import { useEffect, useState } from 'react';
import { api } from '../api/client';

function pill(status) {
  const map = { PENDENTE: 'pill-pendente', ACEITA: 'pill-aceita', RECUSADA: 'pill-recusada' };
  const label = { PENDENTE: 'Pendente', ACEITA: 'Aceita', RECUSADA: 'Recusada' };
  return <span className={`pill ${map[status] || ''}`}>{label[status] || status}</span>;
}

export default function Propostas() {
  const [aba, setAba] = useState('recebidas');
  const [recebidas, setRecebidas] = useState([]);
  const [enviadas, setEnviadas] = useState([]);
  const [toast, setToast] = useState('');
  const [erro, setErro] = useState('');

  function showToast(m) { setToast(m); setTimeout(() => setToast(''), 3000); }

  async function carregar() {
    try {
      const [r, e] = await Promise.all([
        api.get('/propostas/recebidas'),
        api.get('/propostas/enviadas'),
      ]);
      setRecebidas(r);
      setEnviadas(e);
    } catch (ex) { setErro(ex.message); }
  }

  useEffect(() => { carregar(); }, []);

  async function responder(id, aceitar) {
    try {
      await api.post(`/propostas/${id}/${aceitar ? 'aceitar' : 'recusar'}`);
      showToast(aceitar ? '✓ Proposta aceita!' : 'Proposta recusada');
      carregar();
    } catch (e) { setErro(e.message); }
  }

  const lista = aba === 'recebidas' ? recebidas : enviadas;

  return (
    <div className="page">
      {toast && <div className="toast">{toast}</div>}
      <h1>Propostas</h1>

      <div className="subtabs">
        <button className={aba === 'recebidas' ? 'active' : ''} onClick={() => setAba('recebidas')}>Recebidas</button>
        <button className={aba === 'enviadas' ? 'active' : ''} onClick={() => setAba('enviadas')}>Enviadas</button>
      </div>

      {erro && <p className="error">{erro}</p>}

      {lista.map((p) => (
        <div className="card" key={p.id}>
          <div style={{ display: 'flex', gap: 8, marginBottom: 8, alignItems: 'center' }}>
            <span className="tag">⇄ Troca de Sala</span>
            {pill(p.status)}
          </div>
          <strong>
            {aba === 'recebidas'
              ? `${p.solicitanteNome} → Você`
              : `Você → ${p.donoNome}`}
          </strong>
          <div className="info-box" style={{ marginTop: 10, background: '#f0f7ff', borderColor: '#bfdbfe' }}>
            📍 Sala desejada:<br />
            <strong>{p.salaNome}</strong> – {p.dataHoraDesejada}
          </div>
          <p className="muted" style={{ fontStyle: 'italic', marginTop: 8 }}>"{p.justificativa}"</p>

          {aba === 'recebidas' && p.status === 'PENDENTE' && (
            <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
              <button className="btn" style={{ flex: 1 }} onClick={() => responder(p.id, true)}>✓ Aceitar</button>
              <button className="btn btn-danger" style={{ flex: 1 }} onClick={() => responder(p.id, false)}>✗ Recusar</button>
            </div>
          )}
        </div>
      ))}
      {lista.length === 0 && <p className="muted">Nenhuma proposta {aba}.</p>}
    </div>
  );
}
