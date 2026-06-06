import { useState, useEffect } from 'react';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';

const HORARIOS = ['07:00','08:00','09:00','10:00','11:00','12:00','13:00','14:00','15:00','16:00','17:00','18:00','19:00','20:00','21:00','22:00'];

function hoje() {
  return new Date().toISOString().slice(0, 10);
}

export default function Agenda() {
  const { user } = useAuth();
  const [etapa, setEtapa] = useState(1); // 1=data, 2=sala, 3=horario
  const [data, setData] = useState(hoje());
  const [salas, setSalas] = useState([]);
  const [busca, setBusca] = useState('');
  const [sala, setSala] = useState(null);
  const [reservasDia, setReservasDia] = useState([]);
  const [slotSel, setSlotSel] = useState(null);
  const [horaInicio, setHoraInicio] = useState('');
  const [horaFim, setHoraFim] = useState('');
  const [toast, setToast] = useState('');
  const [modal, setModal] = useState(null); // reserva ocupada para troca
  const [erro, setErro] = useState('');

  function showToast(msg) {
    setToast(msg);
    setTimeout(() => setToast(''), 3000);
  }

  // Carrega salas visiveis (visibilidade setorizada pelo curso do usuario)
  async function carregarSalas() {
    try {
      const q = user?.cursoId ? `/salas?cursoId=${user.cursoId}` : '/salas';
      setSalas(await api.get(q));
    } catch (e) { setErro(e.message); }
  }

  async function carregarAgendaDaSala(s) {
    try {
      setReservasDia(await api.get(`/reservas/agenda?salaId=${s.id}&data=${data}`));
    } catch (e) { setErro(e.message); }
  }

  function irParaSalas() {
    carregarSalas();
    setEtapa(2);
  }

  function escolherSala(s) {
    setSala(s);
    setSlotSel(null);
    carregarAgendaDaSala(s);
    setEtapa(3);
  }

  function paraMinutos(hhmm) {
    const [h, m] = hhmm.slice(0, 5).split(':').map(Number);
    return h * 60 + m;
  }

  function slotOcupado(h) {
    const slotIni = paraMinutos(h);
    const slotFim = slotIni + 60; // cada slot representa 1 hora cheia
    return reservasDia.find((r) => {
      const rIni = paraMinutos(r.horaInicio);
      const rFim = paraMinutos(r.horaFim);
      // sobreposicao: reserva comeca antes do slot acabar E termina depois do slot comecar
      return rIni < slotFim && rFim > slotIni;
    });
  }

  function clicarSlot(h) {
    const ocup = slotOcupado(h);
    if (ocup) {
      setModal(ocup); // abre modal de troca
    } else {
      function clicarSlot(h) {
        const ocup = slotOcupado(h);
        if (ocup) {
          setModal(ocup);
    }     else {
          setSlotSel(h);
          setHoraInicio(h);
          const [hh, mm] = h.split(':').map(Number);
          setHoraFim(`${String(hh + 1).padStart(2, '0')}:${String(mm).padStart(2, '0')}`);
    }
  }
    }
  }

  async function reservar() {
    if (!horaInicio || !horaFim) { showToast('Preencha início e término'); return; }
    if (horaFim <= horaInicio) { showToast('O término deve ser depois do início'); return; }
    setErro('');
    try {
      await api.post('/reservas', {
        salaId: sala.id, data,
        horaInicio: `${horaInicio}:00`, horaFim: `${horaFim}:00`,
        tipoReserva: 'GRADE_BIMESTRAL',
      });
      showToast('✓ Reserva criada!');
      carregarAgendaDaSala(sala);
      setHoraInicio(''); setHoraFim('');
    } catch (e) { setErro(e.message); }
  }

  const salasFiltradas = salas.filter((s) =>
    s.nome.toLowerCase().includes(busca.toLowerCase()));

  return (
    <div className="page">
      {toast && <div className="toast">{toast}</div>}

      {etapa === 1 && (
        <>
          <h1>📅 Selecione a Data</h1>
          <div className="date-row">
            <input type="date" value={data} onChange={(e) => setData(e.target.value)} />
            <button className="btn btn-sm" onClick={() => setData(hoje())}>Hoje</button>
          </div>
          <button className="btn" onClick={irParaSalas}>Próximo: Selecionar Sala →</button>
        </>
      )}

      {etapa === 2 && (
        <>
          <h1>🏛 Selecione uma Sala</h1>
          <p className="lead">Data: {data} · mostrando salas do seu curso</p>
          <input placeholder="🔍 Buscar sala..." value={busca} onChange={(e) => setBusca(e.target.value)} />
          <div style={{ marginTop: 16 }}>
            {salasFiltradas.map((s) => (
              <div className="list-item" key={s.id} style={{ cursor: 'pointer' }} onClick={() => escolherSala(s)}>
                <div className="list-item-left">
                  <span className="ic">🏫</span>
                  <div>
                    <strong>{s.nome}</strong>
                    <p className="muted">{s.andar} · {s.capacidade} lugares</p>
                  </div>
                </div>
                <span className="muted">→</span>
              </div>
            ))}
            {salasFiltradas.length === 0 && <p className="muted">Nenhuma sala visível para seu curso.</p>}
          </div>
          <button className="btn btn-outline btn-sm" style={{ marginTop: 12 }} onClick={() => setEtapa(1)}>← Voltar</button>
        </>
      )}

      {etapa === 3 && sala && (
        <>
          <h1>⏰ Escolha o Horário</h1>
          <div className="card">
            <strong>{sala.nome} · {sala.andar}</strong>
            <p className="muted">Data: {data}</p>
          </div>

          <div className="slot-grid">
            {HORARIOS.map((h) => {
              const ocup = slotOcupado(h);
              const cls = ocup ? 'slot ocupado' : (slotSel === h ? 'slot sel' : 'slot');
              return <div key={h} className={cls} onClick={() => clicarSlot(h)}>{h}</div>;
            })}
          </div>

         <label>Horário de início</label>
          <input type="time" value={horaInicio} onChange={(e) => setHoraInicio(e.target.value)} />

          <label>Horário de término</label>
          <input type="time" value={horaFim} onChange={(e) => setHoraFim(e.target.value)} />

          {erro && <p className="error">{erro}</p>}

          <div style={{ display: 'flex', gap: 12, marginTop: 18 }}>
            <button className="btn btn-outline" onClick={() => setEtapa(2)}>← Voltar</button>
            <button className="btn" style={{ flex: 1 }} onClick={reservar}>Reservar ✓</button>
          </div>
        </>
      )}

      {modal && (
        <ModalTroca
          reserva={modal}
          onClose={() => setModal(null)}
          onSucesso={() => { setModal(null); showToast('✓ Proposta de troca enviada!'); }}
        />
      )}
    </div>
  );
}

function ModalTroca({ reserva, onClose, onSucesso }) {
  const [justificativa, setJustificativa] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function enviar() {
    if (!justificativa.trim()) { setErro('A justificativa é obrigatória.'); return; }
    setEnviando(true);
    setErro('');
    try {
      await api.post('/propostas', { reservaOrigemId: reserva.id, justificativa });
      onSucesso();
    } catch (e) { setErro(e.message); setEnviando(false); }
  }

  return (
    <div className="modal-bg" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>⚠️ Horário Ocupado</h3>
        <div className="info-box">
          <strong>{reserva.salaNome}</strong><br />
          {reserva.data} às {reserva.horaInicio.slice(0, 5)}<br />
          Reservado por: <strong>{reserva.solicitanteNome}</strong>
        </div>
        <label>Justificativa para a troca</label>
        <textarea value={justificativa} onChange={(e) => setJustificativa(e.target.value)}
          placeholder="Explique por que precisa desta sala/horário..." />
        {erro && <p className="error">{erro}</p>}
        <div className="modal-actions">
          <button className="btn btn-blue" onClick={enviar} disabled={enviando}>
            ⇄ Propor Troca de Sala
          </button>
          <button className="btn btn-outline" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  );
}
