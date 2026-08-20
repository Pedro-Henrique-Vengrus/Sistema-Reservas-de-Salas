import { useEffect, useRef, useState } from 'react';
import { api } from '../api/client';
import { dataHoraBr } from '../lib/format';
import { EmptyState } from './ui/primitives';

const ICONES = {
  RESERVA_APROVADA: '✓', RESERVA_RECUSADA: '✕', RESERVA_CANCELADA: '⊘', RESERVA_CRIADA: '＋',
  TROCA_RECEBIDA: '⇄', TROCA_ACEITA: '⇄', TROCA_RECUSADA: '⇄', TROCA_CANCELADA: '⇄',
  AMBIENTE_INATIVADO: '🏛', CURSO_INATIVADO: '🎓',
};

/** Sino de avisos do cabecalho: trocas, moderacao e cancelamentos forcados. */
export default function NotificacoesPopover({ naoLidas, aoAtualizar }) {
  const [aberto, setAberto] = useState(false);
  const [itens, setItens] = useState([]);
  const ref = useRef(null);

  useEffect(() => {
    if (!aberto) return undefined;
    api.get('/notificacoes').then(setItens).catch(() => setItens([]));
    const fora = (e) => { if (ref.current && !ref.current.contains(e.target)) setAberto(false); };
    document.addEventListener('mousedown', fora);
    return () => document.removeEventListener('mousedown', fora);
  }, [aberto]);

  async function marcarUma(n) {
    if (n.lida) return;
    await api.post(`/notificacoes/${n.id}/lida`);
    setItens((l) => l.map((x) => (x.id === n.id ? { ...x, lida: true } : x)));
    aoAtualizar?.();
  }

  async function marcarTodas() {
    await api.post('/notificacoes/marcar-todas-lidas');
    setItens((l) => l.map((x) => ({ ...x, lida: true })));
    aoAtualizar?.();
  }

  return (
    <div className="popover-anchor" ref={ref}>
      <button className="icon-btn" onClick={() => setAberto((a) => !a)} title="Notificações" aria-label="Notificações">
        <span aria-hidden>🔔</span>
        {naoLidas > 0 && <span className="dot">{naoLidas > 99 ? '99+' : naoLidas}</span>}
      </button>

      {aberto && (
        <div className="popover">
          <div className="popover-head">
            <strong className="text-md">Notificações</strong>
            {itens.some((n) => !n.lida) && (
              <button className="btn btn-ghost btn-sm" onClick={marcarTodas}>Marcar todas como lidas</button>
            )}
          </div>
          <div className="notif-list">
            {itens.map((n) => (
              <button key={n.id} className={`notif ${n.lida ? '' : 'unread'}`} onClick={() => marcarUma(n)}>
                <span className="ico" aria-hidden>{ICONES[n.tipo] || '•'}</span>
                <span className="grow">
                  <strong className="text-md">{n.titulo}</strong>
                  <span className="text-sm text-muted" style={{ display: 'block' }}>{n.mensagem}</span>
                  <span className="ts">{dataHoraBr(n.dataCriacao)}</span>
                </span>
              </button>
            ))}
            {itens.length === 0 && (
              <EmptyState icone="🔔" titulo="Tudo em dia" descricao="Você não tem avisos no momento." />
            )}
          </div>
        </div>
      )}
    </div>
  );
}
