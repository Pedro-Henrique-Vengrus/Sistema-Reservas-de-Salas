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

    // Abrir o painel ja da os avisos por vistos: o contador zera na hora.
    // A lista mantem o destaque de "nao lida" desta abertura, para o usuario
    // identificar o que chegou desde a ultima vez.
    api.get('/notificacoes')
      .then(async (lista) => {
        setItens(lista);
        if (lista.some((n) => !n.lida)) {
          await api.post('/notificacoes/marcar-todas-lidas');
          aoAtualizar?.();
        }
      })
      .catch(() => setItens([]));

    const fora = (e) => { if (ref.current && !ref.current.contains(e.target)) setAberto(false); };
    document.addEventListener('mousedown', fora);
    return () => document.removeEventListener('mousedown', fora);
  }, [aberto, aoAtualizar]);

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
            <span className="text-sm text-muted">
              {itens.length > 0 ? `${itens.length} aviso(s)` : ''}
            </span>
          </div>
          <div className="notif-list">
            {itens.map((n) => (
              <div key={n.id} className={`notif ${n.lida ? '' : 'unread'}`}>
                <span className="ico" aria-hidden>{ICONES[n.tipo] || '•'}</span>
                <span className="grow">
                  <strong className="text-md">{n.titulo}</strong>
                  <span className="text-sm text-muted" style={{ display: 'block' }}>{n.mensagem}</span>
                  <span className="ts">{dataHoraBr(n.dataCriacao)}</span>
                </span>
              </div>
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
